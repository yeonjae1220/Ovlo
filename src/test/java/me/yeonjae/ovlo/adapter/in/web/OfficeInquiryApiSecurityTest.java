package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.dto.result.AskResult;
import me.yeonjae.ovlo.application.port.in.office.AcceptOfficeInvitationUseCase;
import me.yeonjae.ovlo.application.port.in.office.AdministerOfficeUseCase;
import me.yeonjae.ovlo.application.port.in.office.AskQuestionUseCase;
import me.yeonjae.ovlo.application.port.in.office.GetInquiryQuery;
import me.yeonjae.ovlo.application.port.in.office.GetOfficeQuery;
import me.yeonjae.ovlo.application.port.in.office.GiveInquiryFeedbackUseCase;
import me.yeonjae.ovlo.application.port.in.office.ManageOfficeStaffUseCase;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.domain.office.exception.InquiryException;
import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.OfficeMembership;
import me.yeonjae.ovlo.domain.office.model.OfficeRole;
import me.yeonjae.ovlo.shared.security.JwtTokenProvider;
import me.yeonjae.ovlo.shared.security.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * 질문·피드백·문의 로그 API 경계. 질문은 회원 요청 제한을 먼저 타고, Office 일일 상한은 429 INQUIRY_RATE_LIMITED,
 * 문의 로그는 그 Office 담당자만.
 */
@SpringBootTest
@ActiveProfiles("test")
class OfficeInquiryApiSecurityTest {
    @MockitoBean private me.yeonjae.ovlo.adapter.out.persistence.MemberPersistenceAdapter authenticatedMembers;

    @BeforeEach void activeAuthenticatedMemberFixture() {
        org.mockito.Mockito.when(authenticatedMembers.isActive(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    }


    private static final long OFFICE_ID = 5L;
    private static final long MEMBER_ID = 42L;

    @Autowired private WebApplicationContext wac;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private AskQuestionUseCase askQuestionUseCase;
    @MockitoBean private GiveInquiryFeedbackUseCase feedbackUseCase;
    @MockitoBean private GetInquiryQuery getInquiryQuery;
    @MockitoBean private RateLimiterService rateLimiterService;
    @MockitoBean private GetOfficeQuery getOfficeQuery;
    @MockitoBean private ManageOfficeStaffUseCase manageOfficeStaffUseCase;
    @MockitoBean private AdministerOfficeUseCase administerOfficeUseCase;
    @MockitoBean private AcceptOfficeInvitationUseCase acceptOfficeInvitationUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac).apply(springSecurity()).build();
    }

    private String bearer(OfficeMembership membership) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(new MemberId(MEMBER_ID), MemberRole.MEMBER, membership);
    }

    @Test
    @DisplayName("질문: 요청 제한을 먼저 확인하고 유스케이스에 회원 id 와 언어를 넘긴다 → 201")
    void askDelegates() throws Exception {
        given(askQuestionUseCase.ask(any())).willReturn(new AskResult(900L, InquiryOutcome.ANSWERED, NoticeLang.VI,
                "Hạn chót là 10/2 [1].", List.of(new AskResult.Citation(1, 77L, "Ký túc xá", "https://u.ac.kr/77", null)), true));

        mockMvc.perform(post("/api/v1/office/" + OFFICE_ID + "/inquiries").header("Authorization", bearer(null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Khi nào hết hạn?\",\"lang\":\"vi\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("ANSWERED"))
                .andExpect(jsonPath("$.citations[0].noticeId").value(77));

        verify(rateLimiterService).checkOfficeInquiryRate(MEMBER_ID);
        verify(askQuestionUseCase).ask(org.mockito.ArgumentMatchers.argThat(c ->
                c.memberId().equals(MEMBER_ID) && c.officeId().equals(OFFICE_ID) && c.lang() == NoticeLang.VI));
    }

    @Test
    @DisplayName("Office 일일 상한은 429 INQUIRY_RATE_LIMITED, 빈 질문은 400")
    void limitsAndValidation() throws Exception {
        given(askQuestionUseCase.ask(any())).willThrow(new InquiryException("한도", InquiryException.ErrorType.RATE_LIMITED));

        mockMvc.perform(post("/api/v1/office/" + OFFICE_ID + "/inquiries").header("Authorization", bearer(null))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"q\",\"lang\":\"ko\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("INQUIRY_RATE_LIMITED"));

        mockMvc.perform(post("/api/v1/office/" + OFFICE_ID + "/inquiries").header("Authorization", bearer(null))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("문의 로그는 그 Office 담당자만: 다른 Office 403, 같은 Office 200, 일반 회원 403, 비로그인 401")
    void staffLog() throws Exception {
        given(getInquiryQuery.findForStaff(eq(OFFICE_ID), any(), any(), any(), any())).willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/office-admin/" + OFFICE_ID + "/inquiries")
                        .header("Authorization", bearer(new OfficeMembership(7L, OfficeRole.OFFICE_ADMIN))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/office-admin/" + OFFICE_ID + "/inquiries").header("Authorization", bearer(null)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/office-admin/" + OFFICE_ID + "/inquiries")).andExpect(status().isUnauthorized());
        verify(getInquiryQuery, never()).findForStaff(anyLong(), any(), any(), any(), any());

        mockMvc.perform(get("/api/v1/office-admin/" + OFFICE_ID + "/inquiries").param("outcome", "NO_GROUNDS")
                        .header("Authorization", bearer(new OfficeMembership(OFFICE_ID, OfficeRole.OFFICE_STAFF))))
                .andExpect(status().isOk());
        verify(getInquiryQuery).findForStaff(eq(OFFICE_ID), eq(InquiryOutcome.NO_GROUNDS), any(), any(), any());
    }

    @MockitoBean private me.yeonjae.ovlo.application.port.in.office.GetInquiryConversationQuery conversationQuery;

    @Test void conversationRequiresAuthenticationAndUsesAuthenticatedMember() throws Exception {
        mockMvc.perform(get("/api/v1/office/5/inquiries/900/conversation")).andExpect(status().isUnauthorized());
        given(conversationQuery.conversation(5L, MEMBER_ID, 900L)).willReturn(
                new me.yeonjae.ovlo.application.dto.result.InquiryConversationResult(List.of(), null, true));
        mockMvc.perform(get("/api/v1/office/5/inquiries/900/conversation").header("Authorization", bearer(null)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.canContinue").value(true));
        verify(conversationQuery).conversation(5L, MEMBER_ID, 900L);
    }

    @Test void followUpIdMustBePositiveAndIsPassedToUseCase() throws Exception {
        mockMvc.perform(post("/api/v1/office/5/inquiries").header("Authorization", bearer(null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Where?\",\"lang\":\"en\",\"previousInquiryId\":-1}"))
                .andExpect(status().isBadRequest());
        given(askQuestionUseCase.ask(any())).willReturn(new AskResult(901L, InquiryOutcome.NO_GROUNDS, NoticeLang.EN, null, List.of(), true));
        mockMvc.perform(post("/api/v1/office/5/inquiries").header("Authorization", bearer(null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Where?\",\"lang\":\"en\",\"previousInquiryId\":900}"))
                .andExpect(status().isCreated());
        verify(askQuestionUseCase).ask(new me.yeonjae.ovlo.application.dto.command.AskQuestionCommand(5L, MEMBER_ID, "Where?", NoticeLang.EN, 900L));
    }


    @Test void rejectedRequestHasNoGeneratedAnswerOrHandoffAndGuardOutageIs503() throws Exception {
        given(askQuestionUseCase.ask(any())).willReturn(new AskResult(900L, InquiryOutcome.OUT_OF_SCOPE, NoticeLang.EN, null, List.of(), false));
        mockMvc.perform(post("/api/v1/office/5/inquiries").header("Authorization", bearer(null))
                .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"Write code\",\"lang\":\"en\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.outcome").value("OUT_OF_SCOPE"))
            .andExpect(jsonPath("$.canEscalate").value(false)).andExpect(jsonPath("$.answerText").isEmpty());
        given(askQuestionUseCase.ask(any())).willThrow(new InquiryException("안전한 답변을 확인하지 못했습니다",InquiryException.ErrorType.UNAVAILABLE));
        mockMvc.perform(post("/api/v1/office/5/inquiries").header("Authorization", bearer(null))
                .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"Dormitory?\",\"lang\":\"en\"}"))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("INQUIRY_UNAVAILABLE"));
    }
}
