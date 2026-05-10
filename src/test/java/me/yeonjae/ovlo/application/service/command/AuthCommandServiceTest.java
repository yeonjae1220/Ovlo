package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.LoginCommand;
import me.yeonjae.ovlo.application.dto.command.LogoutCommand;
import me.yeonjae.ovlo.application.dto.command.RefreshTokenCommand;
import me.yeonjae.ovlo.application.dto.result.MemberCredentials;
import me.yeonjae.ovlo.application.dto.result.TokenPairResult;
import me.yeonjae.ovlo.application.port.out.auth.LoadMemberCredentialsPort;
import me.yeonjae.ovlo.application.port.out.auth.PasswordHasherPort;
import me.yeonjae.ovlo.application.port.out.auth.TokenStorePort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.domain.auth.exception.AuthException;
import me.yeonjae.ovlo.domain.auth.model.AuthSession;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import me.yeonjae.ovlo.domain.member.model.MemberRole;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AuthCommandServiceTest {

    @Mock
    private LoadMemberCredentialsPort loadMemberCredentialsPort;
    @Mock
    private PasswordHasherPort passwordHasherPort;
    @Mock
    private TokenStorePort tokenStorePort;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private LoadMemberPort loadMemberPort;

    private AuthCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new AuthCommandService(
                loadMemberCredentialsPort,
                passwordHasherPort,
                tokenStorePort,
                jwtTokenProvider,
                loadMemberPort
        );
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("올바른 이메일/비밀번호로 로그인하면 토큰 쌍을 반환한다")
        void shouldReturnTokenPair_whenValidCredentials() {
            MemberId memberId = new MemberId(1L);
            String email = "test@example.com";
            String rawPassword = "password123";
            String hashedPassword = "$2a$10$hashed";

            given(loadMemberCredentialsPort.findByEmail(email))
                    .willReturn(Optional.of(new MemberCredentials(memberId, hashedPassword, MemberRole.MEMBER)));
            given(passwordHasherPort.matches(rawPassword, hashedPassword)).willReturn(true);
            given(jwtTokenProvider.generateAccessToken(eq(memberId), any(MemberRole.class))).willReturn("access-token");
            given(jwtTokenProvider.generateRefreshToken()).willReturn("refresh-token");

            TokenPairResult result = sut.login(new LoginCommand(email, rawPassword));

            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
            then(tokenStorePort).should().save(any(AuthSession.class));
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인하면 예외가 발생한다")
        void shouldThrow_whenEmailNotFound() {
            given(loadMemberCredentialsPort.findByEmail(anyString())).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.login(new LoginCommand("notfound@example.com", "pw")))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        @Test
        @DisplayName("비밀번호가 틀리면 예외가 발생한다")
        void shouldThrow_whenWrongPassword() {
            MemberId memberId = new MemberId(1L);
            given(loadMemberCredentialsPort.findByEmail(anyString()))
                    .willReturn(Optional.of(new MemberCredentials(memberId, "$2a$10$hashed", MemberRole.MEMBER)));
            given(passwordHasherPort.matches(anyString(), anyString())).willReturn(false);

            assertThatThrownBy(() -> sut.login(new LoginCommand("test@example.com", "wrongpw")))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다");
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("유효한 refresh token으로 로그아웃하면 세션이 삭제된다")
        void shouldDeleteSession_whenValidToken() {
            String refreshToken = "valid-refresh-token";

            sut.logout(new LogoutCommand(refreshToken));

            then(tokenStorePort).should().deleteByRefreshToken(refreshToken);
        }

        @Test
        @DisplayName("존재하지 않는 토큰으로 로그아웃해도 예외 없이 완료된다")
        void shouldCompleteGracefully_whenTokenNotFound() {
            String unknownToken = "unknown-token";

            // deleteByRefreshToken은 없는 토큰을 조용히 무시한다 (이미 로그아웃된 상태)
            assertThatCode(() -> sut.logout(new LogoutCommand(unknownToken)))
                    .doesNotThrowAnyException();
            then(tokenStorePort).should().deleteByRefreshToken(unknownToken);
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class Refresh {

        @Test
        @DisplayName("유효한 refresh token으로 재발급하면 새 토큰 쌍을 반환한다")
        void shouldReturnNewTokenPair_whenValidToken() {
            MemberId memberId = new MemberId(1L);
            String oldRefreshToken = "old-refresh-token";
            AuthSession session = AuthSession.create(memberId, oldRefreshToken,
                    Instant.now().plus(7, ChronoUnit.DAYS));

            given(tokenStorePort.findByRefreshToken(oldRefreshToken)).willReturn(Optional.of(session));
            given(jwtTokenProvider.generateAccessToken(eq(memberId), any(MemberRole.class))).willReturn("new-access-token");
            given(jwtTokenProvider.generateRefreshToken()).willReturn("new-refresh-token");

            TokenPairResult result = sut.refresh(new RefreshTokenCommand(oldRefreshToken));

            assertThat(result.accessToken()).isEqualTo("new-access-token");
            assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
            then(tokenStorePort).should().save(any(AuthSession.class));
        }

        @Test
        @DisplayName("만료된 세션으로 재발급하면 예외가 발생한다")
        void shouldThrow_whenSessionExpired() {
            MemberId memberId = new MemberId(1L);
            String refreshToken = "old-token";
            AuthSession session = AuthSession.create(memberId, refreshToken,
                    Instant.now().plus(7, ChronoUnit.DAYS));
            session.revoke(); // 강제 만료

            given(tokenStorePort.findByRefreshToken(refreshToken)).willReturn(Optional.of(session));

            assertThatThrownBy(() -> sut.refresh(new RefreshTokenCommand(refreshToken)))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("만료되었거나 유효하지 않은 세션입니다");
        }

        @Test
        @DisplayName("존재하지 않는 토큰으로 재발급하면 예외가 발생한다")
        void shouldThrow_whenTokenNotFound() {
            given(tokenStorePort.findByRefreshToken(anyString())).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.refresh(new RefreshTokenCommand("unknown-token")))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("유효하지 않은 리프레시 토큰입니다");
        }
    }
}
