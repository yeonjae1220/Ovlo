package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import me.yeonjae.ovlo.adapter.in.web.dto.request.GoogleLoginRequest;
import me.yeonjae.ovlo.adapter.in.web.dto.request.LoginRequest;
import me.yeonjae.ovlo.adapter.in.web.dto.response.GoogleAuthResponse;
import me.yeonjae.ovlo.adapter.in.web.dto.response.LoginResponse;
import me.yeonjae.ovlo.application.dto.command.GoogleLoginCommand;
import me.yeonjae.ovlo.application.dto.command.LoginCommand;
import me.yeonjae.ovlo.application.dto.command.LogoutCommand;
import me.yeonjae.ovlo.application.dto.command.RefreshTokenCommand;
import me.yeonjae.ovlo.application.dto.result.GoogleLoginResult;
import me.yeonjae.ovlo.application.dto.result.TokenPairResult;
import me.yeonjae.ovlo.application.port.in.auth.GoogleLoginUseCase;
import me.yeonjae.ovlo.application.port.in.auth.LoginUseCase;
import me.yeonjae.ovlo.application.port.in.auth.LogoutUseCase;
import me.yeonjae.ovlo.application.port.in.auth.RefreshTokenUseCase;
import me.yeonjae.ovlo.shared.security.RateLimiterService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "Auth", description = "인증/인가 API")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthApiController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";
    private static final long REFRESH_COOKIE_MAX_AGE = 30L * 24 * 60 * 60; // 30일

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GoogleLoginUseCase googleLoginUseCase;
    private final RateLimiterService rateLimiterService;
    private final Environment environment;
    private final Set<String> trustedProxyIps;

    public AuthApiController(
            LoginUseCase loginUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUseCase logoutUseCase,
            GoogleLoginUseCase googleLoginUseCase,
            RateLimiterService rateLimiterService,
            Environment environment,
            @Value("${ovlo.trusted-proxy-ips:127.0.0.1,::1}") String trustedProxyIpsConfig
    ) {
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.googleLoginUseCase = googleLoginUseCase;
        this.rateLimiterService = rateLimiterService;
        this.environment = environment;
        this.trustedProxyIps = Arrays.stream(trustedProxyIpsConfig.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        rateLimiterService.checkLoginRate(extractClientIp(httpRequest), request.email());
        TokenPairResult result = loginUseCase.login(
                new LoginCommand(request.email(), request.password())
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.refreshToken()).toString())
                .body(new LoginResponse(result.accessToken(), result.memberId()));
    }

    @Operation(summary = "토큰 갱신")
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshTokenCookie,
            HttpServletRequest httpRequest) {
        if (refreshTokenCookie == null || refreshTokenCookie.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        rateLimiterService.checkRefreshRate(extractClientIp(httpRequest));
        TokenPairResult result = refreshTokenUseCase.refresh(
                new RefreshTokenCommand(refreshTokenCookie)
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.refreshToken()).toString())
                .body(new LoginResponse(result.accessToken(), result.memberId()));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshTokenCookie) {
        if (refreshTokenCookie != null && !refreshTokenCookie.isBlank()) {
            logoutUseCase.logout(new LogoutCommand(refreshTokenCookie));
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    @Operation(summary = "Google 소셜 로그인")
    @PostMapping("/google")
    public ResponseEntity<GoogleAuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request,
                                                          HttpServletRequest httpRequest) {
        rateLimiterService.checkLoginRate(extractClientIp(httpRequest), "google");
        GoogleLoginResult result = googleLoginUseCase.loginWithGoogle(
                new GoogleLoginCommand(request.code(), request.redirectUri())
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.refreshToken()).toString())
                .body(new GoogleAuthResponse(result.accessToken(), result.memberId(), result.newMember()));
    }

    private ResponseCookie buildRefreshCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(isProd())
                .sameSite("Lax")
                .maxAge(REFRESH_COOKIE_MAX_AGE)
                .path(REFRESH_COOKIE_PATH)
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(isProd())
                .sameSite("Lax")
                .maxAge(0)
                .path(REFRESH_COOKIE_PATH)
                .build();
    }

    private boolean isProd() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    /**
     * 신뢰 프록시(nginx 등) IP에서 온 요청만 X-Real-IP 헤더를 신뢰.
     */
    private String extractClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxyIps.contains(remoteAddr)) {
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }
        return remoteAddr;
    }
}
