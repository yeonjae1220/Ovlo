package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.port.out.office.TranslationPort;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Regressions for notice response authority and retained own consultations. */
@org.springframework.test.context.TestPropertySource(properties="ovlo.review.post102=true")
class OfficeRetainedAccessIntegrationTest extends OfficeFullReviewIntegrationTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"staff","account","office"})
    void retranslationMustRejectRevokedAuthority(String kind) throws Exception {
        var removed = new java.util.concurrent.atomic.AtomicBoolean();
        doAnswer(inv -> {
            if (removed.compareAndSet(false, true)) {
                switch(kind) {
                    case "staff" -> jdbc.update("UPDATE office_staff SET status='REMOVED' WHERE office_id=? AND member_id=?", notice.getOfficeId(), actor);
                    case "account" -> jdbc.update("UPDATE member SET status='SUSPENDED' WHERE id=?",actor);
                    case "office" -> jdbc.update("UPDATE office SET status='SUSPENDED' WHERE id=?",notice.getOfficeId());
                }
                jdbc.update("UPDATE office_notice SET body='PRIVATE_AFTER_REVOCATION', version=version+1, row_version=row_version+1 WHERE id=?", notice.getId());
            }
            TranslationPort.Request request = inv.getArgument(0);
            return new TranslationPort.Result("Training", "Duration: 1.5", "test");
        }).when(translator).translate(any());
        var result = mvc.perform(post(path+"/retranslate").header("Authorization",token).contentType("application/json")
                .content("{\"revision\":"+notice.getRowVersion()+"}"))
                .andExpect(status().isForbidden()).andReturn().getResponse();
        assertThat(removed.get()).isTrue();
        assertThat(result.getContentAsString()).doesNotContain("PRIVATE_AFTER_REVOCATION");
        
    }
    @org.springframework.beans.factory.annotation.Autowired
    me.yeonjae.ovlo.application.port.out.verification.SaveVerificationCredentialPort credentials;

    @Test void expiredVerificationRetainsOwnConsultationRoutes() throws Exception {
        var consultation = consultationFixture();
        Long student = consultation.getMemberId();
        Long university = offices.findById(notice.getOfficeId()).orElseThrow().getGlobalUnivId();
        credentials.save(me.yeonjae.ovlo.domain.verification.model.VerificationCredential.issueManual(
            student, university, null, "review", "Review fixture", java.time.Instant.now()));
        String studentToken = "Bearer " + jwt.generateAccessToken(
            new me.yeonjae.ovlo.domain.member.model.MemberId(student), me.yeonjae.ovlo.domain.member.model.MemberRole.MEMBER);
        mvc.perform(get("/api/v1/office/me").header("Authorization", studentToken)).andExpect(status().isOk());
        jdbc.update("UPDATE verification_credential SET status='EXPIRED' WHERE member_id=?", student);
        mvc.perform(get("/api/v1/office/me").header("Authorization", studentToken)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/office/my-consultations?size=999").header("Authorization",studentToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(consultation.getId()));
        mvc.perform(get("/api/v1/office/my-consultations/"+consultation.getId()).header("Authorization",studentToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.officeId").value(notice.getOfficeId()));
        mvc.perform(get("/api/v1/office/my-consultations/"+consultation.getId()).header("Authorization",token))
                .andExpect(status().isNotFound()); // Even this school's staff cannot use the student's route.
        mvc.perform(get("/api/v1/office/my-consultations").header("Authorization",token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(get("/api/v1/office/my-consultations")).andExpect(status().isUnauthorized());
        String base = "/api/v1/office/"+notice.getOfficeId()+"/consultations/"+consultation.getId();
        mvc.perform(get(base).header("Authorization", studentToken)).andExpect(status().isOk());
        mvc.perform(get(base+"/messages?lang=vi").header("Authorization", studentToken)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test void retainedHistoryMasksExpiredPrivateDataAndRejectsSuspendedAccounts() throws Exception {
        var c=consultationFixture();
        String auth="Bearer "+jwt.generateAccessToken(new me.yeonjae.ovlo.domain.member.model.MemberId(c.getMemberId()),
                me.yeonjae.ovlo.domain.member.model.MemberRole.MEMBER);
        jdbc.update("UPDATE office_consultation SET personal_data_expires_at=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE id=?",c.getId());
        var body=mvc.perform(get("/api/v1/office/my-consultations/"+c.getId()).header("Authorization",auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.question").value("[Content deleted]"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("PRIVATE_STUDENT_QUESTION","PRIVATE_TRANSCRIPT");
        jdbc.update("UPDATE member SET status='SUSPENDED' WHERE id=?",c.getMemberId());
        mvc.perform(get("/api/v1/office/my-consultations").header("Authorization",auth)).andExpect(status().isUnauthorized());
    }

    me.yeonjae.ovlo.adapter.out.office.AnthropicLlmAdapter liveProvider() {
        return new me.yeonjae.ovlo.adapter.out.office.AnthropicLlmAdapter(
                new me.yeonjae.ovlo.shared.metrics.OfficeMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()),
                System.getenv("ANTHROPIC_API_KEY"),System.getenv("OVLO_LLM_BASE_URL"),
                "claude-opus-5-5","medium",2048,90,false,"claude-sonnet-5-5");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="OVLO_LIVE_LLM",matches="1")
    void liveRetranslationRechecksStaffAfterProvider(boolean revoke) throws Exception {
        for(var lang:me.yeonjae.ovlo.domain.office.model.NoticeLang.values())
            if(lang!=me.yeonjae.ovlo.domain.office.model.NoticeLang.KO && lang!=me.yeonjae.ovlo.domain.office.model.NoticeLang.EN)
                notice.putTranslation(lang,"Existing","Duration 1.5",java.time.Instant.now());
        notice=notices.save(notice);
        var llm=liveProvider();
        var entered=new java.util.concurrent.CountDownLatch(1);
        var changed=new java.util.concurrent.CountDownLatch(1);
        doAnswer(inv -> {
            entered.countDown();
            var translated=llm.translate(inv.getArgument(0));
            assertThat(changed.await(20,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            return translated;
        }).when(translator).translate(any());
        doAnswer(inv -> llm.verifyTranslation(inv.getArgument(0),inv.getArgument(1))).when(verifier).verifyTranslation(any(),any());
        try(var pool=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var future=pool.submit(() -> mvc.perform(post(path+"/retranslate").header("Authorization",token)
                .contentType("application/json").content("{\"revision\":"+notice.getRowVersion()+"}"))
                .andReturn().getResponse());
            try {
                assertThat(entered.await(20,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                if(revoke) jdbc.update("UPDATE office_staff SET status='REMOVED' WHERE office_id=? AND member_id=?",notice.getOfficeId(),actor);
            } finally { changed.countDown(); }
            var response=future.get(200,java.util.concurrent.TimeUnit.SECONDS);
            assertThat(response.getStatus()).isEqualTo(revoke?403:200);
            if(!revoke) assertThat(json.readTree(response.getContentAsString()).path("missingLangs").size()).isZero();
            else assertThat(response.getContentAsString()).doesNotContain("교육은","Duration");
        }
    }

    @Test @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="OVLO_LIVE_LLM",matches="1")
    void liveStudentReadsTranslatedReplyWithoutCurrentSchoolCredential() throws Exception {
        var c=consultationFixture();
        chats.saveMessage(c.getChatRoomId(),actor,"여권을 지참해 주세요.");
        var llm=liveProvider();
        doAnswer(inv -> llm.translate(inv.getArgument(0))).when(translator).translate(any());
        String auth="Bearer "+jwt.generateAccessToken(new me.yeonjae.ovlo.domain.member.model.MemberId(c.getMemberId()),
                me.yeonjae.ovlo.domain.member.model.MemberRole.MEMBER);
        mvc.perform(get("/api/v1/office/my-consultations/"+c.getId()).header("Authorization",auth)).andExpect(status().isOk());
        var body=mvc.perform(get("/api/v1/office/"+c.getOfficeId()+"/consultations/"+c.getId()+"/messages?lang=vi")
                .header("Authorization",auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var messages=json.readTree(body);
        assertThat(messages.get(1).path("translatedText").asText()).isNotBlank();
        assertThat(messages.get(1).path("translationFailed").asBoolean()).isFalse();
    }

}
