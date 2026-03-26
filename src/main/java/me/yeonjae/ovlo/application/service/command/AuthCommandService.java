package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.LoginCommand;
import me.yeonjae.ovlo.application.dto.command.LogoutCommand;
import me.yeonjae.ovlo.application.dto.command.RefreshTokenCommand;
import me.yeonjae.ovlo.application.dto.result.MemberCredentials;
import me.yeonjae.ovlo.application.dto.result.TokenPairResult;
import me.yeonjae.ovlo.application.port.in.auth.LoginUseCase;
import me.yeonjae.ovlo.application.port.in.auth.LogoutUseCase;
import me.yeonjae.ovlo.application.port.in.auth.RefreshTokenUseCase;
import me.yeonjae.ovlo.application.port.out.auth.LoadMemberCredentialsPort;
import me.yeonjae.ovlo.application.port.out.auth.PasswordHasherPort;
import me.yeonjae.ovlo.application.port.out.auth.TokenStorePort;
import me.yeonjae.ovlo.domain.auth.exception.AuthException;
import me.yeonjae.ovlo.domain.auth.model.AuthSession;
import me.yeonjae.ovlo.shared.security.JwtTokenProvider;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Transactional
public class AuthCommandService implements LoginUseCase, LogoutUseCase, RefreshTokenUseCase {

    private static final long REFRESH_TOKEN_TTL_DAYS = 7L;

    private final LoadMemberCredentialsPort loadMemberCredentialsPort;
    private final PasswordHasherPort passwordHasherPort;
    private final TokenStorePort tokenStorePort;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthCommandService(LoadMemberCredentialsPort loadMemberCredentialsPort,
                               PasswordHasherPort passwordHasherPort,
                               TokenStorePort tokenStorePort,
                               JwtTokenProvider jwtTokenProvider) {
        this.loadMemberCredentialsPort = loadMemberCredentialsPort;
        this.passwordHasherPort = passwordHasherPort;
        this.tokenStorePort = tokenStorePort;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public TokenPairResult login(LoginCommand command) {
        MemberCredentials credentials = loadMemberCredentialsPort.findByEmail(command.email())
                .orElseThrow(() -> new AuthException("이메일 또는 비밀번호가 올바르지 않습니다"));

        if (credentials.hashedPassword() == null) {
            throw new AuthException("소셜 로그인 계정입니다. Google 로그인을 사용해 주세요");
        }

        if (!passwordHasherPort.matches(command.rawPassword(), credentials.hashedPassword())) {
            throw new AuthException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(credentials.memberId());
        String refreshToken = jwtTokenProvider.generateRefreshToken();
        Instant expiresAt = Instant.now().plus(REFRESH_TOKEN_TTL_DAYS, ChronoUnit.DAYS);

        AuthSession session = AuthSession.create(credentials.memberId(), refreshToken, expiresAt);
        tokenStorePort.save(session);

        return new TokenPairResult(accessToken, refreshToken, credentials.memberId().value());
    }

    @Override
    public void logout(LogoutCommand command) {
        AuthSession session = tokenStorePort.findByRefreshToken(command.refreshToken())
                .orElseThrow(() -> new AuthException("유효하지 않은 리프레시 토큰입니다"));

        tokenStorePort.delete(session.getMemberId());
    }

    @Override
    public TokenPairResult refresh(RefreshTokenCommand command) {
        AuthSession session = tokenStorePort.findByRefreshToken(command.refreshToken())
                .orElseThrow(() -> new AuthException("유효하지 않은 리프레시 토큰입니다"));

        if (session.isExpired()) {
            throw new AuthException("만료되었거나 유효하지 않은 세션입니다. 다시 로그인해 주세요");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(session.getMemberId());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken();
        Instant newExpiry = Instant.now().plus(REFRESH_TOKEN_TTL_DAYS, ChronoUnit.DAYS);

        session.rotate(newRefreshToken, newExpiry);
        tokenStorePort.save(session);

        return new TokenPairResult(newAccessToken, newRefreshToken, session.getMemberId().value());
    }
}
