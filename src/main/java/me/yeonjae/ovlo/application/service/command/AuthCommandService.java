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
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.domain.auth.exception.AuthException;
import me.yeonjae.ovlo.domain.auth.model.AuthSession;
import me.yeonjae.ovlo.domain.auth.model.RefreshRotationOutcome;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.shared.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Transactional
public class AuthCommandService implements LoginUseCase, LogoutUseCase, RefreshTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthCommandService.class);
    private static final long REFRESH_TOKEN_TTL_DAYS = 30L;

    private final LoadMemberCredentialsPort loadMemberCredentialsPort;
    private final PasswordHasherPort passwordHasherPort;
    private final TokenStorePort tokenStorePort;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoadMemberPort loadMemberPort;

    public AuthCommandService(LoadMemberCredentialsPort loadMemberCredentialsPort,
                               PasswordHasherPort passwordHasherPort,
                               TokenStorePort tokenStorePort,
                               JwtTokenProvider jwtTokenProvider,
                               LoadMemberPort loadMemberPort) {
        this.loadMemberCredentialsPort = loadMemberCredentialsPort;
        this.passwordHasherPort = passwordHasherPort;
        this.tokenStorePort = tokenStorePort;
        this.jwtTokenProvider = jwtTokenProvider;
        this.loadMemberPort = loadMemberPort;
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

        String accessToken = jwtTokenProvider.generateAccessToken(credentials.memberId(), credentials.role());
        String refreshToken = jwtTokenProvider.generateRefreshToken();
        Instant expiresAt = Instant.now().plus(REFRESH_TOKEN_TTL_DAYS, ChronoUnit.DAYS);

        AuthSession session = AuthSession.create(credentials.memberId(), refreshToken, expiresAt);
        tokenStorePort.save(session);

        return new TokenPairResult(accessToken, refreshToken, credentials.memberId().value());
    }

    @Override
    public void logout(LogoutCommand command) {
        tokenStorePort.deleteByRefreshToken(command.refreshToken());
    }

    @Override
    public TokenPairResult refresh(RefreshTokenCommand command) {
        AuthSession session = tokenStorePort.findByRefreshToken(command.refreshToken())
                .orElseThrow(() -> new AuthException("유효하지 않은 리프레시 토큰입니다"));

        if (session.isExpired()) {
            throw new AuthException("만료되었거나 유효하지 않은 세션입니다. 다시 로그인해 주세요");
        }

        String newRefreshToken = jwtTokenProvider.generateRefreshToken();
        Instant newExpiry = Instant.now().plus(REFRESH_TOKEN_TTL_DAYS, ChronoUnit.DAYS);
        session.rotate(newRefreshToken, newExpiry);

        // 조회와 교체 사이에 다른 요청이 같은 토큰으로 먼저 교체했을 수 있다. 저장소가 제출 토큰을
        // 현재 토큰과 원자적으로 비교해 판정하므로, 같은 토큰으로 시작한 요청 중 하나만 통과한다.
        RefreshRotationOutcome outcome = tokenStorePort.rotate(session, command.refreshToken());
        switch (outcome) {
            case ROTATED -> { }
            case CONCURRENT -> throw new AuthException(AuthException.ErrorType.CONFLICT,
                    "토큰 재발급이 동시에 진행되었습니다. 다시 시도해 주세요");
            case REUSED -> {
                // 재사용 감지 1단계: 기록만 하고 세션은 유지한다. iOS WebView가 Set-Cookie를 저장하지
                // 못하고 직전 토큰을 다시 보내는 정상 사용자(GLOBAL-PIT-051)와 탈취를 아직 구분하지 못해서다.
                log.warn("Refresh token reuse detected: sessionId={}, memberId={}",
                        session.getId().value(), session.getMemberId().value());
                throw new AuthException("유효하지 않은 리프레시 토큰입니다");
            }
            case INVALID -> throw new AuthException("유효하지 않은 리프레시 토큰입니다");
        }

        MemberRole role = loadMemberPort.findById(session.getMemberId())
                .map(m -> m.getRole())
                .orElse(MemberRole.MEMBER);
        String newAccessToken = jwtTokenProvider.generateAccessToken(session.getMemberId(), role);

        return new TokenPairResult(newAccessToken, newRefreshToken, session.getMemberId().value());
    }
}
