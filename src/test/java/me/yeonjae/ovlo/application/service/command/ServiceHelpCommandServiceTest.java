package me.yeonjae.ovlo.application.service.command;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import me.yeonjae.ovlo.application.dto.command.AskServiceHelpCommand;
import me.yeonjae.ovlo.application.dto.result.ServiceHelpAnswerResult;
import me.yeonjae.ovlo.application.port.out.help.LoadHelpArticlesPort;
import me.yeonjae.ovlo.application.port.out.help.ServiceHelpAdmissionPort;
import me.yeonjae.ovlo.application.port.out.help.ServiceHelpLlmPort;
import me.yeonjae.ovlo.application.port.out.office.InquiryAdmissionPort;
import me.yeonjae.ovlo.application.service.office.GroundedAnswerValidator;
import me.yeonjae.ovlo.domain.help.model.HelpAnswerOutcome;
import me.yeonjae.ovlo.domain.help.model.HelpArticle;
import me.yeonjae.ovlo.domain.office.exception.InquiryException;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ServiceHelpCommandServiceTest {

    private static final long MEMBER_ID = 7L;
    private static final List<HelpArticle> ARTICLES = List.of(
            new HelpArticle("account-delete", "회원 탈퇴", "프로필 > 설정에서 탈퇴할 수 있습니다."),
            new HelpArticle("block-user", "차단하기", "프로필의 메뉴에서 차단할 수 있습니다."));

    @Mock LoadHelpArticlesPort articles;
    @Mock ServiceHelpLlmPort llm;
    @Mock ServiceHelpAdmissionPort admission;
    @Mock InquiryAdmissionPort.Lease lease;

    private ServiceHelpCommandService service;

    @BeforeEach
    void setUp() {
        service = new ServiceHelpCommandService(articles, llm, admission, new GroundedAnswerValidator(),
                new SimpleMeterRegistry());
    }

    private static AskServiceHelpCommand ask(String question) {
        return new AskServiceHelpCommand(MEMBER_ID, question, NoticeLang.KO);
    }

    private void admitted() {
        given(articles.loadAll()).willReturn(ARTICLES);
        given(admission.acquireServiceHelp(MEMBER_ID)).willReturn(lease);
    }

    private static ServiceHelpLlmPort.Verdict allow() {
        return new ServiceHelpLlmPort.Verdict(true, "m", 10, 1);
    }

    private static ServiceHelpLlmPort.Verdict deny() {
        return new ServiceHelpLlmPort.Verdict(false, "m", 10, 1);
    }

    @Test
    @DisplayName("근거 있는 답변은 인용한 도움말 문서를 id·제목과 함께 돌려준다")
    void answered_returnsCitedArticles() {
        admitted();
        given(llm.screenHelp(anyString())).willReturn(allow());
        given(llm.answerHelp(any())).willReturn(new ServiceHelpLlmPort.Answer("설정에서 탈퇴합니다. [1]", "m", 100, 20, false));
        given(llm.verifyHelp(any(), anyString())).willReturn(allow());

        ServiceHelpAnswerResult result = service.ask(ask("탈퇴는 어떻게 하나요?"));

        assertThat(result.outcome()).isEqualTo(HelpAnswerOutcome.ANSWERED);
        assertThat(result.answerText()).isEqualTo("설정에서 탈퇴합니다. [1]");
        assertThat(result.citations()).containsExactly(
                new ServiceHelpAnswerResult.Citation(1, "account-delete", "회원 탈퇴"));
        verify(lease).complete();
    }

    @Test
    @DisplayName("모든 도움말 문서를 번호 붙은 근거로 넘기고 질문은 앞뒤 공백을 지운다")
    void answer_receivesEveryArticleAsNumberedReference() {
        admitted();
        given(llm.screenHelp(anyString())).willReturn(allow());
        given(llm.answerHelp(any())).willReturn(new ServiceHelpLlmPort.Answer("NO_GROUNDS", "m", 1, 1, false));

        service.ask(ask("  차단하면 어떻게 되나요?  "));

        ArgumentCaptor<ServiceHelpLlmPort.Request> request = ArgumentCaptor.forClass(ServiceHelpLlmPort.Request.class);
        verify(llm).answerHelp(request.capture());
        assertThat(request.getValue().question()).isEqualTo("차단하면 어떻게 되나요?");
        assertThat(request.getValue().lang()).isEqualTo(NoticeLang.KO);
        assertThat(request.getValue().references()).containsExactly(
                new ServiceHelpLlmPort.Reference(1, "회원 탈퇴", "프로필 > 설정에서 탈퇴할 수 있습니다."),
                new ServiceHelpLlmPort.Reference(2, "차단하기", "프로필의 메뉴에서 차단할 수 있습니다."));
    }

    @Test
    @DisplayName("서비스와 무관한 질문은 답변 생성 없이 OUT_OF_SCOPE")
    void screenDenied_isOutOfScopeWithoutGeneration() {
        admitted();
        given(llm.screenHelp(anyString())).willReturn(deny());

        ServiceHelpAnswerResult result = service.ask(ask("파이썬 코드 짜줘"));

        assertThat(result.outcome()).isEqualTo(HelpAnswerOutcome.OUT_OF_SCOPE);
        assertThat(result.answerText()).isNull();
        verify(llm, never()).answerHelp(any());
        verify(lease).usage(10, 1);
        verify(lease).complete();
    }

    @Test
    @DisplayName("모델이 NO_GROUNDS 라고 답하면 검증 없이 NO_GROUNDS")
    void modelNoGrounds_isNoGrounds() {
        admitted();
        given(llm.screenHelp(anyString())).willReturn(allow());
        given(llm.answerHelp(any())).willReturn(new ServiceHelpLlmPort.Answer("NO_GROUNDS", "m", 1, 1, false));

        ServiceHelpAnswerResult result = service.ask(ask("다크 모드가 있나요?"));

        assertThat(result.outcome()).isEqualTo(HelpAnswerOutcome.NO_GROUNDS);
        assertThat(result.citations()).isEmpty();
        verify(llm, never()).verifyHelp(any(), anyString());
    }

    @Test
    @DisplayName("없는 문서 번호를 인용하면 NO_GROUNDS")
    void outOfRangeCitation_isNoGrounds() {
        admitted();
        given(llm.screenHelp(anyString())).willReturn(allow());
        given(llm.answerHelp(any())).willReturn(new ServiceHelpLlmPort.Answer("설정에서 바꿉니다. [9]", "m", 1, 1, false));

        assertThat(service.ask(ask("비밀번호 변경?")).outcome()).isEqualTo(HelpAnswerOutcome.NO_GROUNDS);
        verify(llm, never()).verifyHelp(any(), anyString());
    }

    @Test
    @DisplayName("모델이 응답을 거부하면 NO_GROUNDS")
    void refused_isNoGrounds() {
        admitted();
        given(llm.screenHelp(anyString())).willReturn(allow());
        given(llm.answerHelp(any())).willReturn(new ServiceHelpLlmPort.Answer("", "m", 1, 0, true));

        assertThat(service.ask(ask("탈퇴?")).outcome()).isEqualTo(HelpAnswerOutcome.NO_GROUNDS);
    }

    @Test
    @DisplayName("독립 검증이 거부하면 답변을 내보내지 않는다")
    void verifyDenied_isNoGrounds() {
        admitted();
        given(llm.screenHelp(anyString())).willReturn(allow());
        given(llm.answerHelp(any())).willReturn(new ServiceHelpLlmPort.Answer("고객센터 전화로 탈퇴합니다. [1]", "m", 1, 1, false));
        given(llm.verifyHelp(any(), anyString())).willReturn(deny());

        ServiceHelpAnswerResult result = service.ask(ask("탈퇴?"));

        assertThat(result.outcome()).isEqualTo(HelpAnswerOutcome.NO_GROUNDS);
        assertThat(result.answerText()).isNull();
    }

    @Test
    @DisplayName("분류 결과가 없으면 실패로 닫고 예약을 돌려주지 않는다")
    void missingVerdict_failsClosed() {
        admitted();
        given(llm.screenHelp(anyString())).willReturn(null);

        assertThatThrownBy(() -> service.ask(ask("탈퇴?")))
                .isInstanceOf(InquiryException.class)
                .extracting("errorType").isEqualTo(InquiryException.ErrorType.UNAVAILABLE);
        verify(lease, never()).complete();
        verify(lease).close();
    }

    @Test
    @DisplayName("유료 호출 전에 예약을 먼저 잡고 모든 호출의 사용량을 기록한다")
    void admissionPrecedesPaidCalls() {
        admitted();
        given(llm.screenHelp(anyString())).willReturn(allow());
        given(llm.answerHelp(any())).willReturn(new ServiceHelpLlmPort.Answer("설정에서 탈퇴합니다. [1]", "m", 100, 20, false));
        given(llm.verifyHelp(any(), anyString())).willReturn(new ServiceHelpLlmPort.Verdict(true, "m", 30, 1));

        service.ask(ask("탈퇴?"));

        InOrder order = inOrder(admission, llm, lease);
        order.verify(admission).acquireServiceHelp(MEMBER_ID);
        order.verify(llm).screenHelp(anyString());
        order.verify(lease).usage(10, 1);
        order.verify(llm).answerHelp(any());
        order.verify(lease).usage(100, 20);
        order.verify(llm).verifyHelp(any(), anyString());
        order.verify(lease).usage(30, 1);
        order.verify(lease).complete();
        order.verify(lease).close();
    }

    @Test
    @DisplayName("빈 질문은 예약·호출 없이 거절한다")
    void blankQuestion_rejectedBeforeAdmission() {
        assertThatThrownBy(() -> service.ask(ask("   "))).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(admission, llm);
    }
}
