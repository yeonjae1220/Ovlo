package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.AskQuestionCommand;
import me.yeonjae.ovlo.application.dto.command.InquiryFeedbackCommand;
import me.yeonjae.ovlo.application.dto.result.AskResult;
import me.yeonjae.ovlo.application.port.out.office.AnswerGenerationPort;
import me.yeonjae.ovlo.application.port.out.office.LoadInquiryPort;
import me.yeonjae.ovlo.application.port.out.office.NoticeSearchPort;
import me.yeonjae.ovlo.application.port.out.office.SaveInquiryPort;
import me.yeonjae.ovlo.application.service.office.GroundedAnswerValidator;
import me.yeonjae.ovlo.application.service.office.StudentOfficeAccessPolicy;
import me.yeonjae.ovlo.domain.office.exception.InquiryException;
import me.yeonjae.ovlo.domain.office.model.Inquiry;
import me.yeonjae.ovlo.domain.office.model.InquiryFeedback;
import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.OfficeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InquiryCommandServiceTest {

    private static final Instant NOW = Instant.parse("2027-03-02T09:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2027, 3, 2);
    private static final Long OFFICE = 5L;
    private static final Long MEMBER = 42L;

    @Mock StudentOfficeAccessPolicy accessPolicy;
    @Mock NoticeSearchPort searchPort;
    @Mock AnswerGenerationPort answerPort;
    @Mock LoadInquiryPort loadInquiryPort;
    @Mock SaveInquiryPort saveInquiryPort;

    private InquiryCommandService sut;
    private Notice dorm;
    private Notice insurance;

    @BeforeEach
    void setUp() {
        sut = new InquiryCommandService(accessPolicy, searchPort, answerPort, new GroundedAnswerValidator(),
                loadInquiryPort, saveInquiryPort, Clock.fixed(NOW, ZoneOffset.UTC), 8, 500);
        Office office = Office.create(266L, "국제처", NOW);
        office.assignId(new OfficeId(OFFICE));
        lenient().when(accessPolicy.assertMember(MEMBER, OFFICE)).thenReturn(office);
        lenient().when(loadInquiryPort.countByOfficeSince(eq(OFFICE), any())).thenReturn(0L);
        lenient().when(saveInquiryPort.save(any())).thenAnswer(inv -> {
            Inquiry i = inv.getArgument(0);
            if (i.getId() == null) i.assignId(900L);
            return i;
        });
        dorm = approved(77L, "기숙사 신청 안내", "2027-1학기 기숙사 신청은 2월 10일까지입니다.");
        dorm.putTranslation(NoticeLang.EN, "Dormitory application", "Applications close on February 10.", NOW);
        insurance = approved(78L, "보험 가입 안내", "입국 후 2주 안에 보험에 가입해야 합니다.");
    }

    private static Notice approved(Long id, String title, String body) {
        Notice n = Notice.register(OFFICE, NoticeCategory.HOUSING, title, body, "https://univ.ac.kr/" + id, List.of(), "2027-1",
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 30), 11L, NOW);
        n.assignId(id);
        n.approve("11", NOW, LocalDate.of(2027, 2, 1));
        return n;
    }

    @Test
    @DisplayName("후보에서 답하면 ANSWERED 로 기록하고, 인용은 학생 언어 제목·출처·만료일과 함께 돌아온다")
    void answered() {
        given(searchPort.findCandidates(eq(OFFICE), eq(NoticeLang.EN), anyString(), eq(TODAY), anyInt()))
                .willReturn(List.of(dorm, insurance));
        given(answerPort.answer(any())).willReturn(new AnswerGenerationPort.Result(
                "Applications close on February 10 [1].", "claude-test", 500, 40, false));

        AskResult r = sut.ask(new AskQuestionCommand(OFFICE, MEMBER, "When does dorm application close?", NoticeLang.EN));

        assertThat(r.outcome()).isEqualTo(InquiryOutcome.ANSWERED);
        assertThat(r.answerText()).isEqualTo("Applications close on February 10 [1].");
        assertThat(r.citations()).singleElement().satisfies(c -> {
            assertThat(c.index()).isEqualTo(1);
            assertThat(c.noticeId()).isEqualTo(77L);
            assertThat(c.title()).isEqualTo("Dormitory application");
            assertThat(c.sourceUrl()).isEqualTo("https://univ.ac.kr/77");
            assertThat(c.expiresAt()).isEqualTo(LocalDate.of(2027, 6, 30));
        });
        assertThat(r.canEscalate()).isTrue();

        ArgumentCaptor<AnswerGenerationPort.Request> req = ArgumentCaptor.forClass(AnswerGenerationPort.Request.class);
        verify(answerPort).answer(req.capture());
        // 영어 질문: 영어 번역이 있는 후보만 근거 자격이 있고(도메인 판정), 본문은 영어 번역
        assertThat(req.getValue().candidates()).extracting(AnswerGenerationPort.Candidate::body)
                .containsExactly("Applications close on February 10.");

        ArgumentCaptor<Inquiry> saved = ArgumentCaptor.forClass(Inquiry.class);
        verify(saveInquiryPort).save(saved.capture());
        assertThat(saved.getValue().getCitedNoticeIds()).containsExactly(77L);
        assertThat(saved.getValue().getCandidateNoticeIds()).containsExactly(77L);   // 78 은 영어 번역이 없어 제외
        assertThat(saved.getValue().getModelId()).isEqualTo("claude-test");
    }

    @Test
    @DisplayName("후보 밖 인용은 NO_GROUNDS 로 강등해 기록한다 (모델 ID 도 남긴다)")
    void degradesOnBadCitation() {
        given(searchPort.findCandidates(anyLong(), any(), anyString(), any(), anyInt())).willReturn(List.of(dorm));
        given(answerPort.answer(any())).willReturn(new AnswerGenerationPort.Result("2월 10일까지입니다 [4].", "claude-test", 1, 1, false));

        AskResult r = sut.ask(new AskQuestionCommand(OFFICE, MEMBER, "기숙사 신청 언제까지?", NoticeLang.KO));

        assertThat(r.outcome()).isEqualTo(InquiryOutcome.NO_GROUNDS);
        assertThat(r.answerText()).isNull();
        ArgumentCaptor<Inquiry> saved = ArgumentCaptor.forClass(Inquiry.class);
        verify(saveInquiryPort).save(saved.capture());
        assertThat(saved.getValue().getOutcome()).isEqualTo(InquiryOutcome.NO_GROUNDS);
        assertThat(saved.getValue().getModelId()).isEqualTo("claude-test");
        assertThat(saved.getValue().getCandidateNoticeIds()).containsExactly(77L);
    }

    @Test
    @DisplayName("후보가 없으면 LLM 을 부르지 않고 NO_GROUNDS 로 기록한다; 만료 공지는 후보에서 빠진다")
    void noCandidatesSkipsModel() {
        Notice expired = approved(79L, "지난 공지", "작년 신청");
        expired.expire(LocalDate.of(2027, 7, 1));
        given(searchPort.findCandidates(anyLong(), any(), anyString(), any(), anyInt())).willReturn(List.of(expired));

        AskResult r = sut.ask(new AskQuestionCommand(OFFICE, MEMBER, "신청 언제?", NoticeLang.KO));

        assertThat(r.outcome()).isEqualTo(InquiryOutcome.NO_GROUNDS);
        verify(answerPort, never()).answer(any());
        verify(saveInquiryPort).save(any());
    }

    @Test
    @DisplayName("모델이 거부(refusal)하면 NO_GROUNDS 로 기록한다")
    void refusalRecorded() {
        given(searchPort.findCandidates(anyLong(), any(), anyString(), any(), anyInt())).willReturn(List.of(dorm));
        given(answerPort.answer(any())).willReturn(new AnswerGenerationPort.Result("", "claude-test", 1, 0, true));

        AskResult r = sut.ask(new AskQuestionCommand(OFFICE, MEMBER, "기숙사?", NoticeLang.KO));

        assertThat(r.outcome()).isEqualTo(InquiryOutcome.NO_GROUNDS);
    }

    @Test
    @DisplayName("Office 일일 상한에 닿으면 RATE_LIMITED 이고 검색·LLM 을 부르지 않는다")
    void officeDailyLimit() {
        given(loadInquiryPort.countByOfficeSince(eq(OFFICE), any())).willReturn(500L);

        assertThatThrownBy(() -> sut.ask(new AskQuestionCommand(OFFICE, MEMBER, "q", NoticeLang.KO)))
                .isInstanceOf(InquiryException.class)
                .extracting(e -> ((InquiryException) e).getErrorType())
                .isEqualTo(InquiryException.ErrorType.RATE_LIMITED);
        verify(searchPort, never()).findCandidates(anyLong(), any(), anyString(), any(), anyInt());
    }

    @Test
    @DisplayName("피드백은 본인 문의에만, 다른 Office 는 NOT_FOUND")
    void feedbackOwnership() {
        Inquiry mine = Inquiry.noGrounds(OFFICE, MEMBER, NoticeLang.KO, "q", List.of(), NOW, 1, "m");
        mine.assignId(900L);
        given(loadInquiryPort.findById(900L)).willReturn(Optional.of(mine));

        sut.feedback(new InquiryFeedbackCommand(OFFICE, 900L, MEMBER, false, "틀렸어요"));
        verify(saveInquiryPort).saveFeedback(new InquiryFeedback(900L, false, "틀렸어요", NOW));

        assertThatThrownBy(() -> sut.feedback(new InquiryFeedbackCommand(OFFICE, 900L, 43L, true, null)))
                .isInstanceOf(InquiryException.class)
                .extracting(e -> ((InquiryException) e).getErrorType())
                .isEqualTo(InquiryException.ErrorType.FORBIDDEN);
        assertThatThrownBy(() -> sut.feedback(new InquiryFeedbackCommand(99L, 900L, MEMBER, true, null)))
                .isInstanceOf(InquiryException.class);
    }
}
