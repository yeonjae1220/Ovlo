package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.port.out.office.*;
import me.yeonjae.ovlo.domain.office.model.*;
import me.yeonjae.ovlo.domain.member.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real security filters and PostgreSQL regression tests for in-flight authority and lock ordering. */
class OfficeInFlightAccessIntegrationTest extends OfficeFullReviewIntegrationTest {
    @Autowired me.yeonjae.ovlo.application.port.out.verification.SaveVerificationCredentialPort credentials;
    @Autowired me.yeonjae.ovlo.application.port.in.office.OpenConsultationUseCase opening;
    @MockitoBean AnswerGenerationPort answers;
    @MockitoBean InquiryGuardPort guard;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    me.yeonjae.ovlo.adapter.out.persistence.OfficeConsultationPersistenceAdapter consultationSpy;
    @Autowired me.yeonjae.ovlo.application.service.office.NoticeAnswerFinalizer finalizer;
    @Autowired org.springframework.transaction.support.TransactionTemplate tx;
    @Autowired OfficePersonalDataPort privacy;
    @Autowired NoticeKnowledgePort knowledge;

    @Test void answerAndStaffNoticeCreationDoNotDeadlockOnMemberForeignKey() throws Exception {
        // A staff member can also be a verified student. Both real write paths must coexist.
        verifyStudent(actor); approveNotice();
        var locked=new java.util.concurrent.CountDownLatch(2);
        try(var pool=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var answer=pool.submit(() -> tx.execute(status -> {
                privacy.lockActiveMember(actor); awaitBoth(locked);
                return finalizer.finish(Inquiry.answered(notice.getOfficeId(),actor,NoticeLang.KO,
                        "교육 시간은?","1.5시간 [1]",java.util.List.of(notice.getId()),java.util.List.of(notice.getId()),
                        Instant.now(),1L,"probe"),java.util.List.of(notice),java.util.List.of(),java.util.List.of());
            }));
            var newNotice=pool.submit(() -> tx.execute(status -> {
                knowledge.lockOffice(notice.getOfficeId()); awaitBoth(locked);
                return notices.save(Notice.register(notice.getOfficeId(),NoticeCategory.OTHER,"새 공지","교육 안내",null,
                        java.util.List.of(),"2026-2",LocalDate.now(),LocalDate.now().plusDays(7),actor,Instant.now()));
            }));
            assertThat(answer.get(20,java.util.concurrent.TimeUnit.SECONDS).getOutcome()).isEqualTo(InquiryOutcome.ANSWERED);
            assertThat(newNotice.get(20,java.util.concurrent.TimeUnit.SECONDS).getId()).isNotNull();
        }
    }

    private static void awaitBoth(java.util.concurrent.CountDownLatch latch) {
        latch.countDown();
        try { assertThat(latch.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue(); }
        catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
    }

    me.yeonjae.ovlo.adapter.out.office.AnthropicLlmAdapter liveProvider() {
        return new me.yeonjae.ovlo.adapter.out.office.AnthropicLlmAdapter(
                new me.yeonjae.ovlo.shared.metrics.OfficeMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()),
                System.getenv("ANTHROPIC_API_KEY"),System.getenv("OVLO_LLM_BASE_URL"),
                "claude-opus-5-5","medium",2048,90,false,"claude-sonnet-5-5");
    }

    void approveNotice() {
        notice.approve(actor.toString(),Instant.now(),LocalDate.now(ZoneOffset.UTC));
        me.yeonjae.ovlo.support.NoticeFixtures.verify(notice,Instant.now());
        notices.save(notice);
        notice=notices.findCommittedById(notice.getId()).orElseThrow();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="OVLO_LIVE_LLM",matches="1")
    void liveAnswerWithConcurrentConsultationOrRevocation(boolean revoke) throws Exception {
        var c=consultationFixture();
        jdbc.update("UPDATE office_consultation SET status='CLOSED' WHERE id=?",c.getId());
        verifyStudent(c.getMemberId()); approveNotice();
        var llm=liveProvider();
        when(guard.screen(anyString(),anyList())).thenAnswer(i -> llm.screen(i.getArgument(0),i.getArgument(1)));
        lenient().when(guard.verify(any(),anyString())).thenAnswer(i -> llm.verify(i.getArgument(0),i.getArgument(1)));
        var entered=new java.util.concurrent.CountDownLatch(1);
        var changed=new java.util.concurrent.CountDownLatch(1);
        when(answers.answer(any())).thenAnswer(i -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            entered.countDown();
            var answer=llm.answer(i.getArgument(0));
            assertThat(changed.await(30,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(answer.modelId()).contains("sonnet");
            return answer;
        });
        String auth="Bearer "+jwt.generateAccessToken(new MemberId(c.getMemberId()),MemberRole.MEMBER);
        try(var pool=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var request=pool.submit(() -> mvc.perform(post("/api/v1/office/"+notice.getOfficeId()+"/inquiries")
                    .header("Authorization",auth).contentType("application/json")
                    .content("{\"question\":\"학교 교육 시간은 얼마인가요?\",\"lang\":\"ko\"}")).andReturn().getResponse());
            try {
                assertThat(entered.await(100,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                if(revoke) jdbc.update("UPDATE verification_credential SET status='EXPIRED' WHERE member_id=?",c.getMemberId());
                else opening.open(new me.yeonjae.ovlo.application.dto.command.OpenConsultationCommand(
                        notice.getOfficeId(),c.getMemberId(),null,"v2","비자 상담 부탁드립니다.",NoticeLang.KO));
            } finally { changed.countDown(); }
            var response=request.get(150,java.util.concurrent.TimeUnit.SECONDS);
            assertThat(response.getStatus()).isEqualTo(revoke?403:201);
            if(revoke) {
                assertThat(response.getContentAsString()).doesNotContain("ANSWERED","1.5");
                assertThat(jdbc.queryForObject("SELECT count(*) FROM office_inquiry WHERE member_id=?",Long.class,c.getMemberId())).isZero();
            } else {
                assertThat(json.readTree(response.getContentAsString()).path("outcome").asText()).isEqualTo("ANSWERED");
                assertThat(jdbc.queryForObject("SELECT count(*) FROM office_consultation WHERE member_id=? AND status='OPEN'",Long.class,c.getMemberId())).isEqualTo(1);
            }
        }
    }

    @Test @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="OVLO_LIVE_LLM",matches="1")
    void liveRemovedStaffCannotReceiveTranslation() throws Exception {
        var c=consultationFixture(); var llm=liveProvider();
        var entered=new java.util.concurrent.CountDownLatch(1); var changed=new java.util.concurrent.CountDownLatch(1);
        doAnswer(i -> {
            entered.countDown(); var translated=llm.translate(i.getArgument(0));
            assertThat(changed.await(30,java.util.concurrent.TimeUnit.SECONDS)).isTrue(); return translated;
        }).when(translator).translate(any());
        try(var pool=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var request=pool.submit(() -> mvc.perform(get("/api/v1/office-admin/"+notice.getOfficeId()+"/consultations/"+c.getId()+"/messages?lang=ko")
                    .header("Authorization",token)).andReturn().getResponse());
            try {
                assertThat(entered.await(100,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                jdbc.update("UPDATE office_staff SET status='REMOVED' WHERE office_id=? AND member_id=?",notice.getOfficeId(),actor);
            } finally { changed.countDown(); }
            var response=request.get(120,java.util.concurrent.TimeUnit.SECONDS);
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(response.getContentAsString()).doesNotContain("Tôi", "translatedText");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM chat_message_translation t JOIN message m ON m.id=t.message_id WHERE m.chat_room_id=?",Long.class,c.getChatRoomId())).isZero();
        }
    }

    @Test @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="OVLO_LIVE_LLM",matches="1")
    void liveFrenchDirectConsultationAndStaffReply() throws Exception {
        var c=consultationFixture(); verifyStudent(c.getMemberId());
        jdbc.update("UPDATE office_consultation SET status='CLOSED' WHERE id=?",c.getId());
        var llm=liveProvider(); doAnswer(i -> llm.translate(i.getArgument(0))).when(translator).translate(any());
        var opened=opening.open(new me.yeonjae.ovlo.application.dto.command.OpenConsultationCommand(
                notice.getOfficeId(),c.getMemberId(),null,"v2","Bonjour, comment renouveler mon visa ?",NoticeLang.FR));
        String endpoint="/api/v1/office-admin/"+notice.getOfficeId()+"/consultations/"+opened.id()+"/messages?lang=ko";
        var response=json.readTree(mvc.perform(get(endpoint).header("Authorization",token)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(response.get(0).path("translatedText").asText()).contains("비자");
        chats.saveMessage(opened.chatRoomId(),actor,"여권을 지참하고 국제처를 방문해 주세요.");
        String auth="Bearer "+jwt.generateAccessToken(new MemberId(c.getMemberId()),MemberRole.MEMBER);
        var reply=json.readTree(mvc.perform(get("/api/v1/office/"+notice.getOfficeId()+"/consultations/"+opened.id()+"/messages?lang=fr")
                .header("Authorization",auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(reply.get(1).path("translatedText").asText().toLowerCase()).contains("passeport");
        mvc.perform(get(endpoint).header("Authorization",token)).andExpect(status().isOk());
        verify(translator,times(2)).translate(any());
    }

    void verifyStudent(Long student) {
        credentials.save(me.yeonjae.ovlo.domain.verification.model.VerificationCredential.issueManual(
                student, offices.findById(notice.getOfficeId()).orElseThrow().getGlobalUnivId(),
                null,"review-test","synthetic",Instant.now()));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"staff", "office", "account"})
    void removedStaffCannotReceiveInFlightTranscript(String kind) throws Exception {
        Consultation c=consultationFixture();
        doAnswer(inv -> {
            switch(kind) {
                case "staff" -> jdbc.update("UPDATE office_staff SET status='REMOVED' WHERE office_id=? AND member_id=?",notice.getOfficeId(),actor);
                case "office" -> jdbc.update("UPDATE office SET status='SUSPENDED' WHERE id=?",notice.getOfficeId());
                case "account" -> jdbc.update("UPDATE member SET status='SUSPENDED' WHERE id=?",actor);
            }
            return new TranslationPort.Result("message","비자 도움 필요","probe");
        }).when(translator).translate(any());
        String endpoint="/api/v1/office-admin/"+notice.getOfficeId()+"/consultations/"+c.getId()+"/messages?lang=ko";
        String body=mvc.perform(get(endpoint).header("Authorization",token)).andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(body).doesNotContain("Tôi cần giúp đỡ", "비자 도움 필요");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM chat_message_translation t JOIN message m ON m.id=t.message_id WHERE m.chat_room_id=?",Long.class,c.getChatRoomId())).isZero();
        mvc.perform(get(endpoint).header("Authorization",token)).andExpect(status().is4xxClientError());
    }

    @Test void directForeignConsultationIsTranslated() throws Exception {
        Consultation fixture=consultationFixture();
        // Close the fixture so the real direct-open use case can create a new consultation.
        jdbc.update("UPDATE office_consultation SET status='CLOSED' WHERE id=?",fixture.getId());
        verifyStudent(fixture.getMemberId());
        String studentToken="Bearer "+jwt.generateAccessToken(new MemberId(fixture.getMemberId()),MemberRole.MEMBER);
        String endpoint="/api/v1/office/"+notice.getOfficeId()+"/consultations";
        mvc.perform(post(endpoint).header("Authorization",studentToken).contentType("application/json")
                .content("{\"consentVersion\":\"v2\",\"message\":\"Bonjour\",\"lang\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
        var opened=json.readTree(mvc.perform(post(endpoint).header("Authorization",studentToken).contentType("application/json")
                .content("{\"consentVersion\":\"v2\",\"message\":\"Bonjour, comment renouveler mon visa ?\",\"lang\":\"fr\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        assertThat(opened.path("lang").asText()).isEqualTo("FR");
        doAnswer(inv -> { TranslationPort.Request request=inv.getArgument(0); assertThat(request.from()).isEqualTo(NoticeLang.FR); return new TranslationPort.Result("message","비자 갱신은 어떻게 하나요?","probe"); }).when(translator).translate(any());
        clearInvocations(translator);
        String body=mvc.perform(get("/api/v1/office-admin/"+notice.getOfficeId()+"/consultations/"+opened.path("id").asLong()+"/messages?lang=ko")
                .header("Authorization",token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(body).get(0).path("translatedText").asText()).isNotBlank();
        verify(translator).translate(any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"screen,credential","answer,credential","verify,credential","answer,office","answer,account"})
    void revokedCredentialCannotReceiveGeneratedAnswer(String stage,String kind) throws Exception {
        Consultation c=consultationFixture();
        verifyStudent(c.getMemberId());
        notice.approve(actor.toString(),Instant.now(),LocalDate.now(ZoneOffset.UTC));
        me.yeonjae.ovlo.support.NoticeFixtures.verify(notice,Instant.now());
        notices.save(notice);
        Runnable revoke=() -> {
            switch(kind) {
                case "credential" -> jdbc.update("UPDATE verification_credential SET status='EXPIRED' WHERE member_id=?",c.getMemberId());
                case "office" -> jdbc.update("UPDATE office SET status='SUSPENDED' WHERE id=?",notice.getOfficeId());
                case "account" -> jdbc.update("UPDATE member SET status='SUSPENDED' WHERE id=?",c.getMemberId());
            }
        };
        when(guard.screen(anyString(),anyList())).thenAnswer(inv -> { if(stage.equals("screen")) revoke.run(); return new InquiryGuardPort.Verdict(true,"probe",1,1); });
        lenient().when(guard.verify(any(),anyString())).thenAnswer(inv -> { if(stage.equals("verify")) revoke.run(); return new InquiryGuardPort.Verdict(true,"probe",1,1); });
        lenient().when(answers.answer(any())).thenAnswer(inv -> {
            if(stage.equals("answer")) revoke.run();
            return new AnswerGenerationPort.Result("교육은 1.5시간입니다. [1]","probe",1,1,false);
        });
        String studentToken="Bearer "+jwt.generateAccessToken(new MemberId(c.getMemberId()),MemberRole.MEMBER);
        String endpoint="/api/v1/office/"+notice.getOfficeId()+"/inquiries";
        String body=mvc.perform(post(endpoint).header("Authorization",studentToken).contentType("application/json")
                .content("{\"question\":\"교육 시간은 얼마인가요?\",\"lang\":\"ko\"}"))
                .andExpect(status().isForbidden()).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("ANSWERED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM office_inquiry WHERE member_id=?",Long.class,c.getMemberId())).isZero();
        verify(answers,times(stage.equals("screen")?0:1)).answer(any());
        mvc.perform(post(endpoint).header("Authorization",studentToken).contentType("application/json")
                .content("{\"question\":\"교육 시간은 얼마인가요?\",\"lang\":\"ko\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test void answerAndDirectConsultationBothCommit() throws Exception {
        Consultation fixture=consultationFixture();
        jdbc.update("UPDATE office_consultation SET status='CLOSED' WHERE id=?",fixture.getId());
        verifyStudent(fixture.getMemberId());
        notice.approve(actor.toString(),Instant.now(),LocalDate.now(ZoneOffset.UTC));
        me.yeonjae.ovlo.support.NoticeFixtures.verify(notice,Instant.now());
        notices.save(notice);
        notice=notices.findCommittedById(notice.getId()).orElseThrow();
        var studentLocked=new java.util.concurrent.CountDownLatch(1);
        
        doAnswer(inv -> {
            inv.callRealMethod(); studentLocked.countDown();
            long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
            while(jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE wait_event_type='Lock' AND query LIKE '%SELECT status FROM member%'",Long.class)==0) {
                jdbc.execute("SELECT pg_stat_clear_snapshot()");
                assertThat(System.nanoTime()).isLessThan(deadline);
                Thread.sleep(20);
            }
            return null;
        }).when(consultationSpy).lockStudentForCreation(fixture.getMemberId());
        var failures=new java.util.concurrent.CopyOnWriteArrayList<Throwable>();
        try(var pool=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var answer=pool.submit(() -> { try {
                assertThat(studentLocked.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                finalizer.finish(Inquiry.answered(notice.getOfficeId(),fixture.getMemberId(),NoticeLang.KO,
                        "교육 시간은?","1.5시간 [1]",java.util.List.of(notice.getId()),java.util.List.of(notice.getId()),
                        Instant.now(),1L,"probe"),java.util.List.of(notice),java.util.List.of(),java.util.List.of());
            } catch(Throwable e) { failures.add(e); } });
            var open=pool.submit(() -> { try {
                opening.open(new me.yeonjae.ovlo.application.dto.command.OpenConsultationCommand(
                        notice.getOfficeId(),fixture.getMemberId(),null,"v2","비자 상담 부탁드립니다."));
            } catch(Throwable e) { failures.add(e); } });
            answer.get(20,java.util.concurrent.TimeUnit.SECONDS);
            open.get(20,java.util.concurrent.TimeUnit.SECONDS);
        }
        assertThat(failures).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM office_inquiry WHERE member_id=? AND outcome='ANSWERED'",Long.class,fixture.getMemberId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM office_consultation WHERE member_id=? AND status='OPEN'",Long.class,fixture.getMemberId())).isEqualTo(1);
    }
}
