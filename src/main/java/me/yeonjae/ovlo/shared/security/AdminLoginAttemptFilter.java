package me.yeonjae.ovlo.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * {@code POST /admin/login} 앞단에서 lockout을 강제하는 필터.
 *
 * <p>현재 IP/계정이 {@link AdminLoginAttemptService}에 lockout 상태로 기록돼 있으면
 * 비밀번호 검증(UsernamePasswordAuthenticationFilter)에 도달하기 전에 429 +
 * Retry-After로 차단한다. 실패 카운트 자체는 {@link AdminAuthenticationFailureHandler}가
 * 담당하므로, lockout을 유발한 그 시도까지는 정상 실패 처리되고 이후 시도부터
 * 이 필터가 막는다(lab-dashboard 콘솔과 동일한 의미).
 */
public class AdminLoginAttemptFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/admin/login";

    private final AdminLoginAttemptService attemptService;
    private final ClientIpResolver clientIpResolver;

    public AdminLoginAttemptFilter(AdminLoginAttemptService attemptService,
                                   ClientIpResolver clientIpResolver) {
        this.attemptService = attemptService;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isLoginSubmission(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = clientIpResolver.resolve(request);
        String username = request.getParameter("username");
        AdminLoginAttemptService.LockStatus status = attemptService.checkLock(clientIp, username);
        if (status.locked()) {
            writeLockedResponse(response, status.retryAfterSeconds());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isLoginSubmission(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "POST".equalsIgnoreCase(request.getMethod()) && uri != null && uri.endsWith(LOGIN_PATH);
    }

    private void writeLockedResponse(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(429); // 429 Too Many Requests
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType("text/html;charset=UTF-8");
        long minutes = Math.max(1, (retryAfterSeconds + 59) / 60);
        response.getWriter().write(
                "<!DOCTYPE html><html lang=\"ko\"><head><meta charset=\"UTF-8\">"
                + "<title>로그인 차단</title></head><body style=\"font-family:sans-serif\">"
                + "<h1>로그인 시도가 너무 많습니다</h1>"
                + "<p>약 " + minutes + "분 후 다시 시도해 주세요.</p>"
                + "</body></html>");
    }
}
