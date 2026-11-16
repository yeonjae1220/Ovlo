package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.port.out.office.NoticeTranslationJobPort;
import me.yeonjae.ovlo.application.port.in.office.TranslateNoticeUseCase;
import me.yeonjae.ovlo.application.port.out.office.TranslationPort;
import me.yeonjae.ovlo.application.service.office.NoticeTranslationGate;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Regression coverage for historical evidence, suspension recovery and French duration parsing. */
@org.springframework.boot.test.context.SpringBootTest(properties="ovlo.test.remaining-review=true")
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class NoticeReviewRemainingIssuesIntegrationTest extends NoticeRetranslationIntegrationTest {
    @Autowired NoticeTranslationJobPort jobs;
    @Autowired TranslateNoticeUseCase translations;
    @Autowired me.yeonjae.ovlo.application.service.office.NoticeGovernanceService governance;

    @Test void suspendedThenReactivatedOfficeRecoversUnattemptedTranslation() {
        notice.approve(notice.getCreatedBy().toString(), Instant.now(), LocalDate.now());
        me.yeonjae.ovlo.support.NoticeFixtures.verify(notice, Instant.now());
        for (var lang:NoticeLang.ALL) if (lang!=NoticeLang.EN && lang!=NoticeLang.KO)
            notice.putTranslation(lang,"fixture","fixture",Instant.now());
        notices.save(notice);
        jobs.enqueue(notice.getId(),notice.getVersion(),List.of(NoticeLang.EN));
        var office=offices.findById(notice.getOfficeId()).orElseThrow();
        office.suspend(); offices.save(office);
        assertThat(jobs.dueNotices()).doesNotContain(notice.getId());
        assertThat(jdbc.queryForObject("SELECT state FROM notice_translation_job WHERE notice_id=?",String.class,notice.getId())).isEqualTo("PAUSED");
        office.activate(); offices.save(office);
        // Manual recovery works immediately, even before the scheduler's next tick.
        var result=translations.retryManually(notice.getId());
        assertThat(result.generated()).containsExactly(NoticeLang.EN);
        assertThat(result.deferred()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT state FROM notice_translation_job WHERE notice_id=?",String.class,notice.getId())).isEqualTo("DONE");
        assertThat(jdbc.queryForObject("SELECT attempts FROM notice_translation_job WHERE notice_id=?",Integer.class,notice.getId())).isEqualTo(1);
        verify(translator,times(1)).translate(any());
    }

    @Test void validFrenchDurationPassesStructureValidation() {
        var gate=new NoticeTranslationGate();
        var request=new TranslationPort.Request(NoticeLang.KO,NoticeLang.FR,"교육 안내","교육은 2시간입니다.");
        assertThat(gate.check(request,new TranslationPort.Result("Formation","La formation dure 2 heures.","test")).passed()).isTrue();
        var abbreviated=gate.check(request,new TranslationPort.Result("Formation","La formation dure 2 h.","test"));
        assertThat(abbreviated.passed()).isTrue();
    }

    @Test void historicalAnswerMarksQuarantinedTranslationAsOutdatedEvidence() {
        notice.approve(notice.getCreatedBy().toString(),Instant.now(),LocalDate.now());
        me.yeonjae.ovlo.support.NoticeFixtures.verify(notice,Instant.now());
        notice.putTranslation(NoticeLang.EN,"Training","Training lasts 15 hours.",Instant.now());
        notices.save(notice);
        String fingerprint=me.yeonjae.ovlo.application.service.office.NoticeTranslationEvidence.fingerprint(notice,NoticeLang.EN);
        jdbc.update("UPDATE office_notice_translation SET integrity_verified=false WHERE notice_id=? AND lang='en'",notice.getId());
        var current=notices.findCommittedById(notice.getId()).orElseThrow();
        assertThat(current.isGroundingEligible(LocalDate.now(),NoticeLang.EN)).isFalse();
        var history=mock(me.yeonjae.ovlo.application.port.out.office.LoadInquiryPort.class);
        var sources=mock(me.yeonjae.ovlo.application.port.out.office.NoticeKnowledgePort.class);
        var answer=me.yeonjae.ovlo.domain.office.model.Inquiry.answered(current.getOfficeId(),42L,NoticeLang.EN,
                "How long is training?","Training lasts 15 hours [1].",List.of(current.getId()),List.of(current.getId()),Instant.now(),0,"historical-model");
        answer.assignId(101L);
        when(history.findById(101L)).thenReturn(java.util.Optional.of(answer));
        when(sources.citations(101L)).thenReturn(List.of(new me.yeonjae.ovlo.domain.office.model.NoticeCitation(
                1,current.getId(),current.getVersion(),"Training",null,current.getSource().type(),current.getSource().department(),
                current.getReview().verifiedOn(),current.getExpiresAt(),fingerprint)));
        var query=new me.yeonjae.ovlo.application.service.query.InquiryConversationQueryService(history,notices,
                mock(me.yeonjae.ovlo.application.port.out.office.LoadConsultationPort.class),
                mock(me.yeonjae.ovlo.application.service.office.StudentOfficeAccessPolicy.class),sources,governance,Clock.systemUTC());
        var shown=query.conversation(current.getOfficeId(),42L,101L).turns().getFirst().result();
        assertThat(shown.answerText()).isEqualTo("Training lasts 15 hours [1].");
        assertThat(shown.citations().getFirst().currentlyValid()).isFalse();
        jdbc.update("UPDATE office_notice_translation SET integrity_verified=true WHERE notice_id=? AND lang='en'",notice.getId());
        assertThat(query.conversation(current.getOfficeId(),42L,101L).turns().getFirst().result().citations().getFirst().currentlyValid()).isTrue();
        var corrected=notices.findCommittedById(notice.getId()).orElseThrow();
        corrected.putTranslation(NoticeLang.EN,"Training","Training lasts 1.5 hours.",Instant.now());notices.save(corrected);
        assertThat(query.conversation(current.getOfficeId(),42L,101L).turns().getFirst().result().citations().getFirst().currentlyValid()).isFalse();
    }

    @Test void automaticResumePreservesAttemptsLifetimeAndCooldownAndNeverRevivesExhaustedWork() {
        notice.approve(notice.getCreatedBy().toString(),Instant.now(),LocalDate.now());notices.save(notice);
        jobs.enqueue(notice.getId(),notice.getVersion(),List.of(NoticeLang.EN,NoticeLang.FR,NoticeLang.DE));
        jdbc.update("UPDATE notice_translation_job SET state='PAUSED',attempts=1,available_at=CURRENT_TIMESTAMP+INTERVAL '1 hour' WHERE notice_id=?",notice.getId());
        jdbc.update("UPDATE notice_translation_job SET attempts=3 WHERE notice_id=? AND lang='fr'",notice.getId());
        jdbc.update("UPDATE notice_translation_job SET created_at=CURRENT_TIMESTAMP-INTERVAL '49 hours' WHERE notice_id=? AND lang='de'",notice.getId());
        var before=jdbc.queryForList("SELECT attempts,available_at,created_at FROM notice_translation_job WHERE notice_id=? AND lang='en'",notice.getId());
        assertThat(jobs.dueNotices()).doesNotContain(notice.getId());
        assertThat(jdbc.queryForList("SELECT attempts,available_at,created_at FROM notice_translation_job WHERE notice_id=? AND lang='en'",notice.getId())).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT state FROM notice_translation_job WHERE notice_id=? AND lang='fr'",String.class,notice.getId())).isEqualTo("FAILED");
        assertThat(jdbc.queryForObject("SELECT state FROM notice_translation_job WHERE notice_id=? AND lang='de'",String.class,notice.getId())).isEqualTo("CANCELLED");
        jdbc.update("UPDATE notice_translation_job SET available_at=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE notice_id=? AND lang='en'",notice.getId());
        assertThat(jobs.dueNotices()).contains(notice.getId());
        assertThat(jobs.claim(notice.getId(),notice.getVersion(),NoticeLang.EN)).isPresent();
        assertThat(jobs.claim(notice.getId(),notice.getVersion(),NoticeLang.FR)).isEmpty();
        assertThat(jobs.claim(notice.getId(),notice.getVersion(),NoticeLang.DE)).isEmpty();
        verifyNoInteractions(translator);
    }
}
