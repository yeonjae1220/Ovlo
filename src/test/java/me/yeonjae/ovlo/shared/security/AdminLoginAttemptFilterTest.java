package me.yeonjae.ovlo.shared.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AdminLoginAttemptFilterTest {

    @Mock
    private AdminLoginAttemptService attemptService;

    @Mock
    private ClientIpResolver clientIpResolver;

    @Mock
    private FilterChain filterChain;

    private AdminLoginAttemptFilter filter() {
        return new AdminLoginAttemptFilter(attemptService, clientIpResolver);
    }

    @Test
    @DisplayName("POST /admin/login 이 아니면 락 검사 없이 통과시킨다")
    void nonLoginRequest_passesThroughWithoutLockCheck() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(attemptService);
    }

    @Test
    @DisplayName("GET /admin/login(폼 표시)은 가드 대상이 아니다")
    void getLoginPage_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(attemptService);
    }

    @Test
    @DisplayName("락이 없으면 POST /admin/login 을 인증 체인으로 통과시킨다")
    void postLogin_notLocked_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/login");
        request.addParameter("username", "admin@example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(clientIpResolver.resolve(request)).willReturn("203.0.113.9");
        given(attemptService.checkLock("203.0.113.9", "admin@example.com"))
                .willReturn(AdminLoginAttemptService.LockStatus.unlocked());

        filter().doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("락이 걸려 있으면 429 + Retry-After 로 차단하고 인증 체인을 진행하지 않는다")
    void postLogin_locked_returns429AndBlocks() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/login");
        request.addParameter("username", "admin@example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(clientIpResolver.resolve(request)).willReturn("203.0.113.9");
        given(attemptService.checkLock("203.0.113.9", "admin@example.com"))
                .willReturn(new AdminLoginAttemptService.LockStatus(true, 840L));

        filter().doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("840");
        verify(filterChain, never()).doFilter(any(), any());
    }
}
