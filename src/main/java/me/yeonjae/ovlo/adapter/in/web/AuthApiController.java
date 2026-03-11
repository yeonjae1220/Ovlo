package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import me.yeonjae.ovlo.adapter.in.web.dto.request.LoginRequest;
import me.yeonjae.ovlo.adapter.in.web.dto.request.RefreshTokenRequest;
import me.yeonjae.ovlo.application.dto.command.LoginCommand;
import me.yeonjae.ovlo.application.dto.command.LogoutCommand;
import me.yeonjae.ovlo.application.dto.command.RefreshTokenCommand;
import me.yeonjae.ovlo.application.dto.result.TokenPairResult;
import me.yeonjae.ovlo.application.port.in.auth.LoginUseCase;
import me.yeonjae.ovlo.application.port.in.auth.LogoutUseCase;
import me.yeonjae.ovlo.application.port.in.auth.RefreshTokenUseCase;
import me.yeonjae.ovlo.shared.security.RateLimiterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Auth", description = "인증/인가 API")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthApiController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RateLimiterService rateLimiterService;

    public AuthApiController(
            LoginUseCase loginUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUseCase logoutUseCase,
            RateLimiterService rateLimiterService
    ) {
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.rateLimiterService = rateLimiterService;
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<TokenPairResult> login(@Valid @RequestBody LoginRequest request,
                                                 HttpServletRequest httpRequest) {
        rateLimiterService.checkLoginRate(extractClientIp(httpRequest));
        TokenPairResult result = loginUseCase.login(
                new LoginCommand(request.email(), request.password())
        );
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "토큰 갱신")
    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResult> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                                   HttpServletRequest httpRequest) {
        rateLimiterService.checkRefreshRate(extractClientIp(httpRequest));
        TokenPairResult result = refreshTokenUseCase.refresh(
                new RefreshTokenCommand(request.refreshToken())
        );
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        logoutUseCase.logout(new LogoutCommand(request.refreshToken()));
        return ResponseEntity.noContent().build();
    }

    /**
     * nginx가 신뢰 프록시로서 X-Real-IP를 설정하므로 이 헤더를 우선 사용.
     * X-Forwarded-For는 클라이언트가 임의 설정 가능해 Rate Limit 우회 수단이 됨.
     */
    private String extractClientIp(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
