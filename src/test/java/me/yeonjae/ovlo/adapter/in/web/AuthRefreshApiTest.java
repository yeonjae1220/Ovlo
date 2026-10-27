package me.yeonjae.ovlo.adapter.in.web;

import jakarta.servlet.http.Cookie;
import me.yeonjae.ovlo.application.dto.command.RefreshTokenCommand;
import me.yeonjae.ovlo.application.dto.result.TokenPairResult;
import me.yeonjae.ovlo.application.service.command.AuthCommandService;
import me.yeonjae.ovlo.domain.auth.exception.AuthException;
import me.yeonjae.ovlo.shared.security.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * 재발급 응답 계약을 실제 보안 필터 체인 위에서 고정한다. 프론트 {@code refreshAuth} 는 409 를
 * "동시 재발급에서 짐 — 쿠키를 건드리지 말고 한 번 재시도"로, 401 을 "로그아웃"으로 해석한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthRefreshApiTest {

    @Autowired
    private WebApplicationContext wac;

    // AuthCommandService 가 Login·Logout·Refresh 유스케이스를 함께 구현하므로 클래스째로 대체한다
    // (RefreshTokenUseCase 만 대체하면 같은 빈을 받는 LoginUseCase 주입이 깨진다).
    @MockitoBean
    private AuthCommandService refreshTokenUseCase;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("재발급 성공은 200 과 새 refresh 쿠키를 준다")
    void refresh_rotated_setsNewCookie() throws Exception {
        given(refreshTokenUseCase.refresh(any(RefreshTokenCommand.class)))
                .willReturn(new TokenPairResult("new-access", "new-refresh", 1L));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refresh_token", "old-refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=new-refresh")));
    }

    @Test
    @DisplayName("동시 재발급에서 진 요청은 409 AUTH_CONFLICT 이고 쿠키를 덮어쓰지 않는다")
    void refresh_concurrentLoser_isConflictWithoutCookie() throws Exception {
        given(refreshTokenUseCase.refresh(any(RefreshTokenCommand.class)))
                .willThrow(new AuthException(AuthException.ErrorType.CONFLICT, "동시 재발급"));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refresh_token", "old-refresh")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_CONFLICT"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    @DisplayName("재사용·무효 토큰은 401 AUTH_UNAUTHORIZED")
    void refresh_invalid_isUnauthorized() throws Exception {
        given(refreshTokenUseCase.refresh(any(RefreshTokenCommand.class)))
                .willThrow(new AuthException("유효하지 않은 리프레시 토큰입니다"));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refresh_token", "reused")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }
}
