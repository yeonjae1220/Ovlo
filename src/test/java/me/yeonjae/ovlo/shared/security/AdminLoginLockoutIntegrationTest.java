package me.yeonjae.ovlo.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * 실제 {@code adminFilterChain}을 통한 lockout 동작의 end-to-end 검증.
 *
 * <p>CsrfFilter → {@link AdminLoginAttemptFilter} → UsernamePasswordAuthenticationFilter
 * 의 실제 필터 순서·CSRF·{@link ClientIpResolver} 해석을 그대로 태운다. Redis 카운팅
 * 로직 자체는 {@link AdminLoginAttemptServiceTest}가 단위로 덮으므로, 여기서는
 * 카운터/락 저장소만 mock으로 대체해 "필터·핸들러가 체인에 올바로 설치됐는지"를 증명한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class AdminLoginLockoutIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    @MockitoBean
    private AdminLoginAttemptService attemptService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("lockout 상태면 POST /admin/login 은 비밀번호 검증 전에 429 + Retry-After 로 차단된다")
    void lockedSubmission_blockedWith429() throws Exception {
        given(attemptService.checkLock(anyString(), any()))
                .willReturn(new AdminLoginAttemptService.LockStatus(true, 840L));

        mockMvc.perform(post("/admin/login").with(csrf())
                        .param("username", "admin@test.example.com")
                        .param("password", "irrelevant"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "840"));

        // 차단됐으므로 인증·실패 카운트까지 도달하지 않는다
        verify(attemptService, never()).recordFailure(anyString(), any());
    }

    @Test
    @DisplayName("락이 없고 자격증명이 틀리면 실패를 기록하고 /admin/login?error 로 리다이렉트한다")
    void unlockedBadCredentials_recordsFailureAndRedirects() throws Exception {
        given(attemptService.checkLock(anyString(), any()))
                .willReturn(AdminLoginAttemptService.LockStatus.unlocked());

        mockMvc.perform(post("/admin/login").with(csrf())
                        .param("username", "admin@test.example.com")
                        .param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error"));

        verify(attemptService).recordFailure(anyString(), eq("admin@test.example.com"));
    }

    @Test
    @DisplayName("GET /admin/login 폼은 가드 없이 200 으로 렌더된다")
    void loginPage_isReachableWithoutLockCheck() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk());

        verify(attemptService, never()).checkLock(any(), any());
    }
}
