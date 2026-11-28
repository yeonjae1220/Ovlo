package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.dto.command.AskServiceHelpCommand;
import me.yeonjae.ovlo.application.dto.result.HelpArticleResult;
import me.yeonjae.ovlo.application.dto.result.ServiceHelpAnswerResult;
import me.yeonjae.ovlo.application.port.in.help.AskServiceHelpUseCase;
import me.yeonjae.ovlo.application.port.in.help.ListHelpArticlesQuery;
import me.yeonjae.ovlo.domain.help.model.HelpAnswerOutcome;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.domain.office.exception.InquiryException;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.shared.exception.TooManyRequestsException;
import me.yeonjae.ovlo.shared.security.JwtTokenProvider;
import me.yeonjae.ovlo.shared.security.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/** 도움말 문서는 누구나 읽고, AI 질문은 로그인 회원만 회원별 요청 제한을 거쳐 한다. */
@SpringBootTest
@ActiveProfiles("test")
class ServiceHelpApiSecurityTest {
    private static final long MEMBER_ID = 42L;

    @MockitoBean private me.yeonjae.ovlo.adapter.out.persistence.MemberPersistenceAdapter authenticatedMembers;
    @MockitoBean private AskServiceHelpUseCase askUseCase;
    @MockitoBean private ListHelpArticlesQuery articlesQuery;
    @MockitoBean private RateLimiterService rateLimiterService;

    @Autowired private WebApplicationContext wac;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.when(authenticatedMembers.isActive(any())).thenReturn(true);
        mockMvc = webAppContextSetup(wac).apply(springSecurity()).build();
    }

    private String bearer() {
        return "Bearer " + jwtTokenProvider.generateAccessToken(new MemberId(MEMBER_ID), MemberRole.MEMBER, null);
    }

    @Test
    @DisplayName("도움말 문서 목록은 비로그인도 읽는다")
    void articles_arePublic() throws Exception {
        given(articlesQuery.list()).willReturn(List.of(new HelpArticleResult("account-delete", "회원 탈퇴", "본문")));

        mockMvc.perform(get("/api/v1/help/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("account-delete"))
                .andExpect(jsonPath("$[0].title").value("회원 탈퇴"));
    }

    @Test
    @DisplayName("AI 질문은 비로그인이면 401 이고 유스케이스를 부르지 않는다")
    void ask_requiresLogin() throws Exception {
        mockMvc.perform(post("/api/v1/help/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"탈퇴?\",\"lang\":\"ko\"}"))
                .andExpect(status().isUnauthorized());
        verify(askUseCase, never()).ask(any());
    }

    @Test
    @DisplayName("AI 질문은 요청 제한을 먼저 보고 인증된 회원 id 와 언어를 넘긴다")
    void ask_delegatesWithAuthenticatedMember() throws Exception {
        given(askUseCase.ask(any())).willReturn(new ServiceHelpAnswerResult(HelpAnswerOutcome.ANSWERED, NoticeLang.EN,
                "Open settings. [1]", List.of(new ServiceHelpAnswerResult.Citation(1, "account-delete", "회원 탈퇴"))));

        mockMvc.perform(post("/api/v1/help/ask").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"How to leave?\",\"lang\":\"en\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("ANSWERED"))
                .andExpect(jsonPath("$.citations[0].articleId").value("account-delete"));

        verify(rateLimiterService).checkServiceHelpRate(MEMBER_ID);
        verify(askUseCase).ask(new AskServiceHelpCommand(MEMBER_ID, "How to leave?", NoticeLang.EN));
    }

    @Test
    @DisplayName("빈 질문은 400, 회원 요청 제한은 429, AI 장애는 503")
    void ask_errors() throws Exception {
        mockMvc.perform(post("/api/v1/help/ask").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"  \"}"))
                .andExpect(status().isBadRequest());

        given(askUseCase.ask(any())).willThrow(new InquiryException("장애", InquiryException.ErrorType.UNAVAILABLE));
        mockMvc.perform(post("/api/v1/help/ask").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"탈퇴?\"}"))
                .andExpect(status().isServiceUnavailable());

        willThrow(new TooManyRequestsException("많음")).given(rateLimiterService).checkServiceHelpRate(MEMBER_ID);
        mockMvc.perform(post("/api/v1/help/ask").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"탈퇴?\"}"))
                .andExpect(status().isTooManyRequests());
    }
}
