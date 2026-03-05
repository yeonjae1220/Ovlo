package me.yeonjae.ovlo.domain.auth.model;

import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthSessionTest {

    private MemberId memberId;
    private String refreshToken;
    private Instant futureExpiry;

    @BeforeEach
    void setUp() {
        memberId = new MemberId(1L);
        refreshToken = "valid-refresh-token";
        futureExpiry = Instant.now().plus(7, ChronoUnit.DAYS);
    }

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("필수 정보로 세션을 생성한다")
        void shouldCreate_withValidArgs() {
            AuthSession session = AuthSession.create(memberId, refreshToken, futureExpiry);

            assertThat(session.getMemberId()).isEqualTo(memberId);
            assertThat(session.getRefreshToken()).isEqualTo(refreshToken);
            assertThat(session.getExpiresAt()).isEqualTo(futureExpiry);
            assertThat(session.isRevoked()).isFalse();
        }

        @Test
        @DisplayName("memberId가 null이면 예외가 발생한다")
        void shouldThrow_whenNullMemberId() {
            assertThatThrownBy(() -> AuthSession.create(null, refreshToken, futureExpiry))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("refreshToken이 null이면 예외가 발생한다")
        void shouldThrow_whenNullRefreshToken() {
            assertThatThrownBy(() -> AuthSession.create(memberId, null, futureExpiry))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("refreshToken이 빈 값이면 예외가 발생한다")
        void shouldThrow_whenBlankRefreshToken() {
            assertThatThrownBy(() -> AuthSession.create(memberId, "  ", futureExpiry))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("리프레시 토큰은 빈 값일 수 없습니다");
        }

        @Test
        @DisplayName("expiresAt이 null이면 예외가 발생한다")
        void shouldThrow_whenNullExpiresAt() {
            assertThatThrownBy(() -> AuthSession.create(memberId, refreshToken, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("expiresAt이 과거이면 예외가 발생한다")
        void shouldThrow_whenPastExpiresAt() {
            Instant past = Instant.now().minus(1, ChronoUnit.SECONDS);
            assertThatThrownBy(() -> AuthSession.create(memberId, refreshToken, past))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("만료 시간은 현재 이후여야 합니다");
        }
    }

    @Nested
    @DisplayName("만료 여부")
    class IsExpired {

        @Test
        @DisplayName("만료 시간이 지나지 않았으면 만료되지 않았다")
        void shouldNotBeExpired_whenFutureExpiry() {
            AuthSession session = AuthSession.create(memberId, refreshToken, futureExpiry);
            assertThat(session.isExpired()).isFalse();
        }

        @Test
        @DisplayName("revoke된 세션은 만료된 것으로 간주한다")
        void shouldBeExpired_whenRevoked() {
            AuthSession session = AuthSession.create(memberId, refreshToken, futureExpiry);
            session.revoke();
            assertThat(session.isExpired()).isTrue();
        }
    }

    @Nested
    @DisplayName("무효화(revoke)")
    class Revoke {

        @Test
        @DisplayName("세션을 revoke하면 isRevoked가 true가 된다")
        void shouldRevoke_successfully() {
            AuthSession session = AuthSession.create(memberId, refreshToken, futureExpiry);

            session.revoke();

            assertThat(session.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("이미 revoke된 세션을 다시 revoke하면 예외가 발생한다")
        void shouldThrow_whenAlreadyRevoked() {
            AuthSession session = AuthSession.create(memberId, refreshToken, futureExpiry);
            session.revoke();

            assertThatThrownBy(() -> session.revoke())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 무효화된 세션입니다");
        }
    }

    @Nested
    @DisplayName("토큰 교체(rotate)")
    class Rotate {

        @Test
        @DisplayName("유효한 세션에서 토큰을 교체할 수 있다")
        void shouldRotate_withNewToken() {
            AuthSession session = AuthSession.create(memberId, refreshToken, futureExpiry);
            String newToken = "new-refresh-token";
            Instant newExpiry = Instant.now().plus(7, ChronoUnit.DAYS);

            session.rotate(newToken, newExpiry);

            assertThat(session.getRefreshToken()).isEqualTo(newToken);
            assertThat(session.getExpiresAt()).isEqualTo(newExpiry);
        }

        @Test
        @DisplayName("만료된(revoked) 세션은 rotate할 수 없다")
        void shouldThrow_whenRevokedSession() {
            AuthSession session = AuthSession.create(memberId, refreshToken, futureExpiry);
            session.revoke();

            assertThatThrownBy(() -> session.rotate("new-token", futureExpiry))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("유효하지 않은 세션입니다");
        }

        @Test
        @DisplayName("새 토큰이 null이면 예외가 발생한다")
        void shouldThrow_whenNullNewToken() {
            AuthSession session = AuthSession.create(memberId, refreshToken, futureExpiry);

            assertThatThrownBy(() -> session.rotate(null, futureExpiry))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("새 만료 시간이 과거이면 예외가 발생한다")
        void shouldThrow_whenPastNewExpiry() {
            AuthSession session = AuthSession.create(memberId, refreshToken, futureExpiry);
            Instant past = Instant.now().minus(1, ChronoUnit.SECONDS);

            assertThatThrownBy(() -> session.rotate("new-token", past))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("만료 시간은 현재 이후여야 합니다");
        }
    }
}
