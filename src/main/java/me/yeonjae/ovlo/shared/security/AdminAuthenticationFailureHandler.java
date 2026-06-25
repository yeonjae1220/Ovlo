package me.yeonjae.ovlo.shared.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import java.io.IOException;

/**
 * Admin formLogin 실패 시 {@link AdminLoginAttemptService}에 IP·계정 실패를 기록한 뒤
 * {@code /admin/login?error}로 리다이렉트한다. lockout 강제는
 * {@link AdminLoginAttemptFilter}가 후속 요청에서 담당한다.
 */
public class AdminAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final AdminLoginAttemptService attemptService;
    private final ClientIpResolver clientIpResolver;

    public AdminAuthenticationFailureHandler(AdminLoginAttemptService attemptService,
                                             ClientIpResolver clientIpResolver) {
        super("/admin/login?error");
        this.attemptService = attemptService;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {
        attemptService.recordFailure(clientIpResolver.resolve(request), request.getParameter("username"));
        super.onAuthenticationFailure(request, response, exception);
    }
}
