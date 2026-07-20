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
import me.yeonjae.ovlo.shared.security.ClientIpResolver;
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

@Tag(name = "Auth", description = "인증/인가 API")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthApiController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GoogleLoginUseCase googleLoginUseCase;
    private final RateLimiterService rateLimiterService;
    private final Environment environment;
    private final ClientIpResolver clientIpResolver;
    // 쿠키 수명은 refresh 토큰 실제 만료(jwt.refresh-token-ttl-minutes)와 동일 소스에서 파생 —
    // 별도 하드코딩 상수로 두면 두 값이 드리프트해 쿠키만 살아있는 유령 세션이 생긴다.
    private final long refreshTokenTtlMinutes;

    public AuthApiController(
            LoginUseCase loginUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUseCase logoutUseCase,
            GoogleLoginUseCase googleLoginUseCase,
            RateLimiterService rateLimiterService,
            Environment environment,
            ClientIpResolver clientIpResolver,
            @Value("${jwt.refresh-token-ttl-minutes:43200}") long refreshTokenTtlMinutes
    ) {
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.googleLoginUseCase = googleLoginUseCase;
        this.rateLimiterService = rateLimiterService;
        this.environment = environment;
        this.clientIpResolver = clientIpResolver;
        this.refreshTokenTtlMinutes = refreshTokenTtlMinutes;
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        rateLimiterService.checkLoginRate(clientIpResolver.resolve(httpRequest), request.email());
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
        rateLimiterService.checkRefreshRate(clientIpResolver.resolve(httpRequest));
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
        rateLimiterService.checkLoginRate(clientIpResolver.resolve(httpRequest), "google");
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
                .secure(isSecureCookieRequired())
                .sameSite("Lax")
                .maxAge(refreshTokenTtlMinutes * 60)
                .path(REFRESH_COOKIE_PATH)
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(isSecureCookieRequired())
                .sameSite("Lax")
                .maxAge(0)
                .path(REFRESH_COOKIE_PATH)
                .build();
    }

    private boolean isSecureCookieRequired() {
        return Arrays.stream(environment.getActiveProfiles())
                .noneMatch(p -> p.equalsIgnoreCase("local") || p.equalsIgnoreCase("test"));
    }
}
