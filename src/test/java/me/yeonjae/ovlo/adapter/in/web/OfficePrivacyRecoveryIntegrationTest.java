package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.port.in.office.TranslateNoticeUseCase;
import me.yeonjae.ovlo.application.port.out.office.*;
import me.yeonjae.ovlo.application.service.command.MemberCommandService;
import me.yeonjae.ovlo.application.dto.command.WithdrawMemberCommand;
import me.yeonjae.ovlo.domain.office.model.*;
import me.yeonjae.ovlo.domain.member.model.*;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Privacy and interrupted-translation regression tests, with real PostgreSQL, Redis and HTTP security. */
@SpringBootTest(properties="ovlo.test.review-reproduction=true")
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class OfficePrivacyRecoveryIntegrationTest extends NoticeRetranslationIntegrationTest {
    @Autowired TranslateNoticeUseCase translations;
    @Autowired NoticeTranslationJobPort jobs;
    @Autowired SaveInquiryPort inquiries;
    @Autowired MemberCommandService memberCommands;
    @Autowired SaveConsultationPort consultations;
    @Autowired OfficePersonalDataPort privacy;
    @Autowired MessageTranslationPort messageTranslations;
    @Autowired me.yeonjae.ovlo.application.port.out.chat.SaveChatPort chats;

    @Test void suspendedDuringPaidCallPausesWithoutLosingPaidAttemptAndRecovers() throws Exception { interruptedTranslation(false); }
    @Test @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="OVLO_LIVE_LLM",matches="1")
    void liveLlmSuspendedDuringPaidCallRecovers() throws Exception { interruptedTranslation(true); }
    void interruptedTranslation(boolean live) throws Exception {
        var today=LocalDate.now(me.yeonjae.ovlo.application.service.office.OfficeDates.ZONE);
        notice.approve(notice.getCreatedBy().toString(),Instant.now(),today);
        me.yeonjae.ovlo.support.NoticeFixtures.verify(notice,Instant.now());
        for(var lang:NoticeLang.ALL) if(lang!=NoticeLang.KO && lang!=NoticeLang.EN)
            notice.putTranslation(lang,"fixture","fixture",Instant.now());
        notice=notices.save(notice);
        long id=notice.getId(),officeId=notice.getOfficeId();
        String old=jdbc.queryForObject("SELECT status FROM office WHERE id=?",String.class,officeId);
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        var llm=live?new me.yeonjae.ovlo.adapter.out.office.AnthropicLlmAdapter(
            new me.yeonjae.ovlo.shared.metrics.OfficeMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()),
            System.getenv("ANTHROPIC_API_KEY"),System.getenv("OVLO_LLM_BASE_URL"),"claude-opus-5-5","medium",2048,90,false):null;
        doAnswer(i->{
            var translated=live?llm.translate(i.getArgument(0)):new TranslationPort.Result("Training","Training lasts 1.5 hours.","fixture");
            entered.countDown();assertThat(release.await(20,TimeUnit.SECONDS)).isTrue();
            return translated;
        }).when(translator).translate(any());
        when(verifier.verifyTranslation(any(),any())).thenAnswer(i->!live || llm.verifyTranslation(i.getArgument(0),i.getArgument(1)));
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            var running=pool.submit(()->translations.translateMissing(id));
            try {
                assertThat(entered.await(100,TimeUnit.SECONDS)).isTrue();
                jdbc.update("UPDATE office SET status='SUSPENDED' WHERE id=?",officeId);
            } finally { release.countDown(); }
            assertThat(running.get(110,TimeUnit.SECONDS).generated()).isEmpty();
        }
        assertThat(notices.findCommittedById(id).orElseThrow().getTranslation(NoticeLang.EN)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT state FROM notice_translation_job WHERE notice_id=?",String.class,id)).isEqualTo("PAUSED");
        jdbc.update("UPDATE office SET status=? WHERE id=?",old,officeId);
        assertThat(translations.retryManually(id).generated()).isEmpty(); // Preserve the cooldown.
        assertThat(jdbc.queryForObject("SELECT attempts FROM notice_translation_job WHERE notice_id=?",Integer.class,id)).isEqualTo(1);
        jdbc.update("UPDATE notice_translation_job SET available_at=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE notice_id=?",id);
        assertThat(jobs.dueNotices()).contains(id);
        assertThat(translations.translateMissing(id).generated()).containsExactly(NoticeLang.EN);
        assertThat(jdbc.queryForObject("SELECT attempts FROM notice_translation_job WHERE notice_id=?",Integer.class,id)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT state FROM notice_translation_job WHERE notice_id=?",String.class,id)).isEqualTo("DONE");
        verify(translator,times(2)).translate(any());
    }

    @Test void unconsentedPersonalTextNeverReachesStaffApiCsvOrDashboard() throws Exception {
        long university=jdbc.queryForObject("SELECT global_univ_id FROM office WHERE id=?",Long.class,notice.getOfficeId());
        String unique=UUID.randomUUID().toString().substring(0,8);
        long student=members.save(Member.create(unique,"Student","Seoul",new Email(unique+"@example.com"),new Password("hashed"),
            new UniversityId(university),new Major("IA",DegreeType.BACHELOR,2))).getId().value();
        String question="My email is synthetic-private@example.com. I need confidential campus support.";
        var inquiry=inquiries.save(Inquiry.noGrounds(notice.getOfficeId(),student,NoticeLang.EN,question,List.of(),Instant.now(),0,null));
        inquiries.saveFeedback(new InquiryFeedback(inquiry.getId(),false,"Private contact: synthetic-feedback@example.com",Instant.now()));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM office_consultation WHERE member_id=?",Integer.class,student)).isZero();
        String base="/api/v1/office-admin/"+notice.getOfficeId();
        mvc.perform(get(base+"/inquiries").header("Authorization",token)).andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].question").doesNotExist())
            .andExpect(jsonPath("$.content[0].feedbackReason").doesNotExist());
        String csv=mvc.perform(get(base+"/export/inquiries.csv").header("Authorization",token)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).doesNotContain("synthetic-private@example.com","synthetic-feedback@example.com","question","answerText","feedbackReason");
        var dashboard=mvc.perform(get(base+"/dashboard").header("Authorization",token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(dashboard).doesNotContain("synthetic-private@example.com","synthetic-feedback@example.com");
        memberCommands.withdraw(new WithdrawMemberCommand(student));
        assertThatThrownBy(()->inquiries.save(Inquiry.noGrounds(notice.getOfficeId(),student,NoticeLang.EN,"late result",List.of(),Instant.now(),0,null))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->inquiries.saveFeedback(new InquiryFeedback(inquiry.getId(),true,"late feedback",Instant.now()))).isInstanceOf(RuntimeException.class);
        mvc.perform(get(base+"/inquiries").header("Authorization",token)).andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].question").doesNotExist());
    }

    @Test void withdrawnStudentConsultationSnapshotAndTranslationCacheAreErased() throws Exception {
        long university=jdbc.queryForObject("SELECT global_univ_id FROM office WHERE id=?",Long.class,notice.getOfficeId());
        String unique=UUID.randomUUID().toString().substring(0,8);
        long student=members.save(Member.create(unique,"Student","Seoul",new Email(unique+"@example.com"),new Password("hashed"),
            new UniversityId(university),new Major("IA",DegreeType.BACHELOR,2))).getId().value();
        var room=chats.save(me.yeonjae.ovlo.domain.chat.model.ChatRoom.create(me.yeonjae.ovlo.domain.chat.model.ChatRoomType.GROUP,
            "Synthetic consultation",List.of(new MemberId(student),new MemberId(notice.getCreatedBy()))));
        var old=Instant.now();
        String sensitive="Private support details: synthetic-archived@example.com";
        var consultation=Consultation.open(notice.getOfficeId(),student,null,"v2",
            new Consultation.SharedContext(sensitive,"Saved answer",NoticeLang.EN,true,"Student: "+sensitive),room.getId().value(),old);
        consultation.close(old.plusSeconds(60));
        var saved=consultations.save(consultation);
        var message=chats.saveMessage(room.getId().value(),student,sensitive);
        messageTranslations.save(message.getId().value(),NoticeLang.KO,"개인 정보 번역 사본","fixture");
        memberCommands.withdraw(new WithdrawMemberCommand(student));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM chat_message_translation WHERE message_id=?",Integer.class,message.getId().value())).isZero();
        assertThat(jdbc.queryForObject("SELECT content FROM message WHERE id=?",String.class,message.getId().value())).isEqualTo("[Content deleted]");
        assertThatThrownBy(()->messageTranslations.save(message.getId().value(),NoticeLang.KO,"late private result","fixture")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->consultations.save(saved)).isInstanceOf(RuntimeException.class);
        mvc.perform(get("/api/v1/office-admin/"+notice.getOfficeId()+"/consultations/"+saved.getId()).header("Authorization",token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.question").value("[Content deleted]"))
            .andExpect(jsonPath("$.conversationText").isEmpty());
    }

    @Test void expiredContentIsHiddenBeforeCleanupAndErasedWithoutAffectingRecentContent() throws Exception {
        long student=notice.getCreatedBy();
        long university=jdbc.queryForObject("SELECT global_univ_id FROM office WHERE id=?",Long.class,notice.getOfficeId());
        String unique=UUID.randomUUID().toString().substring(0,8);
        long other=members.save(Member.create(unique,"Other","Seoul",new Email(unique+"@example.com"),new Password("hashed"),
            new UniversityId(university),new Major("IA",DegreeType.BACHELOR,2))).getId().value();
        var old=inquiries.save(Inquiry.noGrounds(notice.getOfficeId(),student,NoticeLang.EN,"EXPIRED_PRIVATE",List.of(),Instant.now(),0,null));
        var recent=inquiries.save(Inquiry.noGrounds(notice.getOfficeId(),student,NoticeLang.EN,"RECENT_PRIVATE",List.of(),Instant.now(),0,null));
        inquiries.saveFeedback(new InquiryFeedback(old.getId(),true,"expired feedback",Instant.now()));
        var room=chats.save(me.yeonjae.ovlo.domain.chat.model.ChatRoom.create(me.yeonjae.ovlo.domain.chat.model.ChatRoomType.GROUP,
            "Expiry test",List.of(new MemberId(student),new MemberId(other))));
        var c=consultations.save(Consultation.open(notice.getOfficeId(),student,old.getId(),"v2",
            new Consultation.SharedContext("EXPIRED_PRIVATE",null,NoticeLang.EN,true,"EXPIRED_TRANSCRIPT"),room.getId().value(),Instant.now()));
        var message=chats.saveMessage(room.getId().value(),student,"EXPIRED_MESSAGE");
        messageTranslations.save(message.getId().value(),NoticeLang.FR,"EXPIRED_TRANSLATION","fixture");
        jdbc.update("UPDATE office_inquiry SET personal_data_expires_at=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE id=?",old.getId());
        jdbc.update("UPDATE office_consultation SET personal_data_expires_at=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE id=?",c.getId());
        mvc.perform(get("/api/v1/office-admin/"+notice.getOfficeId()+"/consultations/"+c.getId()).header("Authorization",token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.question").value("[Content deleted]"));
        mvc.perform(get("/api/v1/office-admin/"+notice.getOfficeId()+"/consultations/"+c.getId()+"/messages").header("Authorization",token))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
        privacy.eraseExpiredBatch();
        assertThat(jdbc.queryForObject("SELECT question FROM office_inquiry WHERE id=?",String.class,old.getId())).isEqualTo("[Content deleted]");
        assertThat(jdbc.queryForObject("SELECT question FROM office_inquiry WHERE id=?",String.class,recent.getId())).isEqualTo("RECENT_PRIVATE");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM office_inquiry_feedback WHERE inquiry_id=?",Integer.class,old.getId())).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM chat_message_translation WHERE message_id=?",Integer.class,message.getId().value())).isZero();
        assertThat(jdbc.queryForObject("SELECT content FROM message WHERE id=?",String.class,message.getId().value())).isEqualTo("[Content deleted]");
        assertThatThrownBy(()->consultations.save(c)).isInstanceOf(RuntimeException.class);
        assertThat(privacy.eraseExpiredBatch()).isZero();
    }
}
