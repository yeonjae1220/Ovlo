package me.yeonjae.ovlo.shared.security;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminLoginAttemptHandlersTest {

    @Mock
    private AdminLoginAttemptService attemptService;

    @Mock
    private ClientIpResolver clientIpResolver;

    @Test
    @DisplayName("인증 실패 시 실패를 기록하고 /admin/login?error 로 리다이렉트한다")
    void failureHandler_recordsFailureAndRedirects() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/login");
        request.addParameter("username", "admin@example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(clientIpResolver.resolve(request)).willReturn("203.0.113.9");

        var handler = new AdminAuthenticationFailureHandler(attemptService, clientIpResolver);
        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad"));

        verify(attemptService).recordFailure("203.0.113.9", "admin@example.com");
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_MOVED_TEMPORARILY);
        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/login?error");
    }

    @Test
    @DisplayName("인증 성공 시 실패 카운터를 초기화하고 대시보드로 리다이렉트한다")
    void successHandler_clearsCountersAndRedirects() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(clientIpResolver.resolve(request)).willReturn("203.0.113.9");
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        given(authentication.getName()).willReturn("admin@example.com");

        var handler = new AdminAuthenticationSuccessHandler(attemptService, clientIpResolver);
        handler.onAuthenticationSuccess(request, response, authentication);

        verify(attemptService).recordSuccess("203.0.113.9", "admin@example.com");
        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/dashboard");
    }
}
