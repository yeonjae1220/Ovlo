package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.adapter.in.scheduler.NoticeTranslationScheduler;
import me.yeonjae.ovlo.adapter.out.persistence.NoticeTranslationJobAdapter;
import me.yeonjae.ovlo.application.port.in.office.TranslateNoticeUseCase;
import me.yeonjae.ovlo.application.port.out.office.*;
import me.yeonjae.ovlo.domain.office.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real PostgreSQL/Flyway and Redis: provider calls run outside transactions and capacity deferral is durable. */
@org.springframework.boot.test.context.SpringBootTest(properties="ovlo.test.translation-recovery=true")
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class NoticeTranslationRecoveryIntegrationTest extends NoticeRetranslationIntegrationTest {
    @Autowired TranslateNoticeUseCase translations;
    @Autowired NoticeTranslationJobPort jobs;

    Notice approved(String title) {
        var n=Notice.register(notice.getOfficeId(),NoticeCategory.OTHER,title,
                "등록비는 15유로입니다. 보증금은 150유로입니다. 등록비는 환불되지 않습니다. 문의 office@university.example https://university.example/help",
                null,List.of(),"2026-2",today().minusDays(1),today().plusDays(30),notice.getCreatedBy(),Instant.now());
        n.approve(notice.getCreatedBy().toString(),Instant.now(),today());
        me.yeonjae.ovlo.support.NoticeFixtures.verify(n,Instant.now());
        for (var lang:NoticeLang.ALL) if (lang!=NoticeLang.EN && lang!=NoticeLang.KO) n.putTranslation(lang,"fixture","fixture",Instant.now());
        return notices.save(n);
    }

    @Test void sameAuthorConcurrentNoticesRecoverWithoutManualRetranslate() throws Exception { concurrent(false); }

    @Test @EnabledIfEnvironmentVariable(named="OVLO_LIVE_LLM",matches="1")
    void realLlmConcurrentNoticesRecoverWithoutManualRetranslate() throws Exception { concurrent(true); }

    void concurrent(boolean live) throws Exception {
        var a=approved("첫 번째 등록 안내"); var b=approved("두 번째 등록 안내");
        var entered=new CountDownLatch(1); var release=new CountDownLatch(1); var first=new AtomicBoolean(true);
        me.yeonjae.ovlo.adapter.out.office.AnthropicLlmAdapter llm=live ? new me.yeonjae.ovlo.adapter.out.office.AnthropicLlmAdapter(
                new me.yeonjae.ovlo.shared.metrics.OfficeMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()),
                System.getenv("ANTHROPIC_API_KEY"),System.getenv("OVLO_LLM_BASE_URL"),"claude-opus-5-5","medium",2048,90,false):null;
        doAnswer(i -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            TranslationPort.Request request=i.getArgument(0);
            if(first.compareAndSet(true,false)) { entered.countDown(); assertThat(release.await(30,TimeUnit.SECONDS)).isTrue(); }
            return live ? llm.translate(request) : new TranslationPort.Result("Registration",
                    "Fee 15 EUR. Deposit 150 EUR. The fee is non-refundable. Contact office@university.example https://university.example/help","test");
        }).when(translator).translate(any());
        when(verifier.verifyTranslation(any(),any())).thenAnswer(i -> !live || llm.verifyTranslation(i.getArgument(0),i.getArgument(1)));
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            var running=pool.submit(()->translations.translateMissing(a.getId()));
            try {
                assertThat(entered.await(10,TimeUnit.SECONDS)).isTrue();
                var deferred=translations.translateMissing(b.getId());
                assertThat(deferred.generated()).isEmpty(); assertThat(deferred.failed()).isEmpty();
                assertThat(deferred.deferred()).containsExactly(NoticeLang.EN);
                assertThat(jdbc.queryForObject("SELECT state FROM notice_translation_job WHERE notice_id=?",String.class,b.getId())).isEqualTo("PENDING");
                verify(translator,times(1)).translate(any());
            } finally { release.countDown(); }
            assertThat(running.get(210,TimeUnit.SECONDS).generated()).containsExactly(NoticeLang.EN);
        }
        jdbc.update("UPDATE notice_translation_job SET available_at=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE notice_id=?",b.getId());
        // New adapter/scheduler instances recover persisted work, with no approval event or HTTP retry.
        assertThat(new NoticeTranslationJobAdapter(jdbc).dueNotices()).contains(b.getId());
        new NoticeTranslationScheduler(translations).run();
        assertThat(notices.findCommittedById(b.getId()).orElseThrow().isGroundingEligible(today(),NoticeLang.EN)).isTrue();
        assertThat(jdbc.queryForList("SELECT state FROM notice_translation_job WHERE notice_id IN (?,?)",String.class,a.getId(),b.getId())).containsOnly("DONE");
        verify(translator,times(2)).translate(any()); verify(verifier,times(2)).verifyTranslation(any(),any());
    }

    @Test void expiredOwnerCannotStoreOrFinishReplacementWorkAndDeferralsDoNotConsumeFailures() {
        var n=approved("소유권 검증"); jobs.enqueue(n.getId(),n.getVersion(),List.of(NoticeLang.EN));
        var old=jobs.claim(n.getId(),n.getVersion(),NoticeLang.EN).orElseThrow();
        assertThat(jobs.claim(n.getId(),n.getVersion(),NoticeLang.EN)).isEmpty();
        jdbc.update("UPDATE notice_translation_job SET lease_until=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE notice_id=?",n.getId());
        var replacement=jobs.claim(n.getId(),n.getVersion(),NoticeLang.EN).orElseThrow();
        assertThat(jobs.isHeld(old)).isFalse(); jobs.finish(old,false); jobs.defer(old,30);
        assertThat(jobs.isHeld(replacement)).isTrue(); jobs.defer(replacement,30);
        assertThat(jdbc.queryForObject("SELECT attempts FROM notice_translation_job WHERE notice_id=?",Integer.class,n.getId())).isZero();
        assertThat(jobs.claim(n.getId(),n.getVersion(),NoticeLang.EN)).isEmpty();
        for(int i=0;i<3;i++) {
            jdbc.update("UPDATE notice_translation_job SET available_at=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE notice_id=?",n.getId());
            var attempt=jobs.claim(n.getId(),n.getVersion(),NoticeLang.EN).orElseThrow();
            assertThat(jobs.beginAttempt(attempt)).isTrue(); jobs.defer(attempt,300);
        }
        jobs.enqueue(n.getId(),n.getVersion(),List.of(NoticeLang.EN));
        assertThat(jobs.claim(n.getId(),n.getVersion(),NoticeLang.EN)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT state FROM notice_translation_job WHERE notice_id=?",String.class,n.getId())).isEqualTo("FAILED");
    }

    @Test void semanticRejectionIsNotSavedOrAutomaticallyRetried() {
        var n=approved("변조 검증");
        doReturn(new TranslationPort.Result("Registration",
                "Fee 150 EUR. Deposit 15 EUR. The fee is refundable. Contact office@university.example https://university.example/help","test")).when(translator).translate(any());
        when(verifier.verifyTranslation(any(),any())).thenReturn(false);
        assertThat(translations.translateMissing(n.getId()).failed()).containsExactly(NoticeLang.EN);
        assertThat(notices.findCommittedById(n.getId()).orElseThrow().getTranslation(NoticeLang.EN)).isEmpty();
        new NoticeTranslationScheduler(translations).run();
        verify(translator,times(1)).translate(any());
        assertThat(jdbc.queryForObject("SELECT state FROM notice_translation_job WHERE notice_id=?",String.class,n.getId())).isEqualTo("REJECTED");
    }

    @Test void unverifiedLegacyMachineTranslationIsNeitherShownNorGrounded() {
        var n=approved("기존 번역 검증"); n.putTranslation(NoticeLang.EN,"Registration","Wrong fee 900",Instant.now()); notices.save(n);
        jdbc.update("UPDATE office_notice_translation SET integrity_verified=false WHERE notice_id=? AND lang='en'",n.getId());
        var read=notices.findCommittedById(n.getId()).orElseThrow();
        assertThat(read.getTranslation(NoticeLang.EN).orElseThrow().isStaleFor(read.getVersion())).isTrue();
        assertThat(read.isGroundingEligible(today(),NoticeLang.EN)).isFalse();
        assertThat(read.translationsToGenerate(NoticeLang.ALL)).containsExactly(NoticeLang.EN);
    }

    @Test void rejectedManualRetrySharesCooldownAndCrashAttemptLimit() {
        var n=approved("수동 재시도 제한");jobs.enqueue(n.getId(),n.getVersion(),List.of(NoticeLang.EN));
        var first=jobs.claim(n.getId(),n.getVersion(),NoticeLang.EN).orElseThrow();
        assertThat(jobs.beginAttempt(first)).isTrue();jobs.finish(first,true);
        jobs.reopenRejected(n.getId(),n.getVersion());assertThat(jobs.claim(n.getId(),n.getVersion(),NoticeLang.EN)).isEmpty();
        jdbc.update("UPDATE notice_translation_job SET available_at=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE notice_id=?",n.getId());
        jobs.reopenRejected(n.getId(),n.getVersion());
        for(int i=0;i<2;i++) {
            var worker=jobs.claim(n.getId(),n.getVersion(),NoticeLang.EN).orElseThrow();
            assertThat(jobs.beginAttempt(worker)).isTrue();
            jdbc.update("UPDATE notice_translation_job SET lease_until=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE notice_id=?",n.getId());
        }
        assertThat(new NoticeTranslationJobAdapter(jdbc).dueNotices()).doesNotContain(n.getId());
        assertThat(jobs.claim(n.getId(),n.getVersion(),NoticeLang.EN)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT attempts FROM notice_translation_job WHERE notice_id=?",Integer.class,n.getId())).isEqualTo(3);
    }

    @Test void draftWaitsForApprovalAndArchivedOrOldVersionJobsNeverRun() {
        jobs.enqueue(notice.getId(),notice.getVersion(),List.of(NoticeLang.EN));
        assertThat(jobs.dueNotices()).doesNotContain(notice.getId());
        assertThat(jdbc.queryForObject("SELECT state FROM notice_translation_job WHERE notice_id=?",String.class,notice.getId())).isEqualTo("PENDING");
        jdbc.update("UPDATE notice_translation_job SET created_at=CURRENT_TIMESTAMP-INTERVAL '49 hours' WHERE notice_id=?",notice.getId());
        jobs.dueNotices();
        notice.approve(notice.getCreatedBy().toString(),Instant.now(),today());notices.save(notice);
        jobs.activateApproved(notice.getId(),notice.getVersion());assertThat(jobs.dueNotices()).contains(notice.getId());
        notice.archive();notices.save(notice);assertThat(jobs.dueNotices()).doesNotContain(notice.getId());
        assertThat(jobs.claim(notice.getId(),notice.getVersion(),NoticeLang.EN)).isEmpty();
        var n=approved("이전 버전");jobs.enqueue(n.getId(),n.getVersion()-1,List.of(NoticeLang.EN));
        assertThat(jobs.dueNotices()).doesNotContain(n.getId());
        assertThat(jobs.claim(n.getId(),n.getVersion()-1,NoticeLang.EN)).isEmpty();
        verifyNoInteractions(translator);
    }

    // CI runs in UTC; eligibility and source verification use the international office's business date.
    private static LocalDate today() { return LocalDate.now(me.yeonjae.ovlo.application.service.office.OfficeDates.ZONE); }
}
