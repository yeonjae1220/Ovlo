package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.dto.command.AppleLoginCommand;
import me.yeonjae.ovlo.application.dto.result.SocialLoginResult;
import me.yeonjae.ovlo.application.port.in.auth.AppleLoginUseCase;
import me.yeonjae.ovlo.domain.auth.exception.AuthException;
import me.yeonjae.ovlo.domain.member.exception.MemberException;
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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/** 실제 보안 필터 체인을 태워 Apple 로그인 엔드포인트의 공개 여부·검증·오류 매핑을 확인한다. */
@SpringBootTest
@ActiveProfiles("test")
class AppleLoginApiTest {

    private static final String URL = "/api/v1/auth/apple";
    private static final String NONCE = "0123456789abcdef0123";

    @Autowired private WebApplicationContext context;
    @MockitoBean private AppleLoginUseCase appleLoginUseCase;
    @MockitoBean private RateLimiterService rateLimiter;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("로그인하지 않은 상태에서 호출할 수 있고, access token 과 httpOnly refresh 쿠키를 준다")
    void anonymousLogin_issuesSession() throws Exception {
        given(appleLoginUseCase.loginWithApple(any()))
                .willReturn(new SocialLoginResult("access", "refresh", 7L, true));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityToken\":\"jwt\",\"nonce\":\"" + NONCE + "\",\"name\":\"홍길동\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.memberId").value(7))
                .andExpect(jsonPath("$.newMember").value(true))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string("Set-Cookie", allOf(containsString("refresh"), containsString("HttpOnly"))));

        verify(appleLoginUseCase).loginWithApple(new AppleLoginCommand("jwt", NONCE, "홍길동"));
        verify(rateLimiter).checkSocialLoginRate(anyString());
        verify(rateLimiter, never()).checkLoginRate(anyString(), anyString());
    }

    @Test
    @DisplayName("nonce 가 없거나 너무 짧으면 서비스에 가기 전에 400 — 401 로 위장되지 않는다")
    void missingNonce_badRequest() throws Exception {
        for (String body : new String[]{
                "{\"identityToken\":\"jwt\"}",
                "{\"identityToken\":\"jwt\",\"nonce\":\"short\"}",
                "{\"nonce\":\"" + NONCE + "\"}"}) {
            mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verify(appleLoginUseCase, never()).loginWithApple(any());
    }

    @Test
    @DisplayName("토큰 검증 실패는 401")
    void invalidToken_unauthorized() throws Exception {
        given(appleLoginUseCase.loginWithApple(any())).willThrow(new AuthException("Apple 인증에 실패했습니다"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityToken\":\"jwt\",\"nonce\":\"" + NONCE + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("다른 방법으로 가입된 이메일이면 409")
    void emailTakenByAnotherProvider_conflict() throws Exception {
        given(appleLoginUseCase.loginWithApple(any())).willThrow(
                new MemberException("이미 다른 방법으로 가입된 이메일입니다", MemberException.ErrorType.CONFLICT));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityToken\":\"jwt\",\"nonce\":\"" + NONCE + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("이미 다른 방법으로 가입된 이메일입니다"));
    }
}
