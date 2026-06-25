package me.yeonjae.ovlo.shared.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;

/**
 * Admin formLogin 성공 시 {@link AdminLoginAttemptService}의 실패 카운터·락을 비우고
 * 대시보드로 리다이렉트한다. 정상 로그인은 누적 실패 흔적을 남기지 않는다.
 */
public class AdminAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final AdminLoginAttemptService attemptService;
    private final ClientIpResolver clientIpResolver;

    public AdminAuthenticationSuccessHandler(AdminLoginAttemptService attemptService,
                                             ClientIpResolver clientIpResolver) {
        this.attemptService = attemptService;
        this.clientIpResolver = clientIpResolver;
        setDefaultTargetUrl("/admin/dashboard");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {
        attemptService.recordSuccess(clientIpResolver.resolve(request), authentication.getName());
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
