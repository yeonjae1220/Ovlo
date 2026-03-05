package me.yeonjae.ovlo.domain.auth.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenPairTest {

    @Test
    @DisplayName("유효한 토큰 쌍을 생성할 수 있다")
    void shouldCreate_withValidTokens() {
        TokenPair pair = new TokenPair("access-token", "refresh-token");

        assertThat(pair.accessToken()).isEqualTo("access-token");
        assertThat(pair.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("accessToken이 null이면 예외가 발생한다")
    void shouldThrow_whenNullAccessToken() {
        assertThatThrownBy(() -> new TokenPair(null, "refresh-token"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("accessToken이 빈 값이면 예외가 발생한다")
    void shouldThrow_whenBlankAccessToken() {
        assertThatThrownBy(() -> new TokenPair("  ", "refresh-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("액세스 토큰은 빈 값일 수 없습니다");
    }

    @Test
    @DisplayName("refreshToken이 null이면 예외가 발생한다")
    void shouldThrow_whenNullRefreshToken() {
        assertThatThrownBy(() -> new TokenPair("access-token", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("refreshToken이 빈 값이면 예외가 발생한다")
    void shouldThrow_whenBlankRefreshToken() {
        assertThatThrownBy(() -> new TokenPair("access-token", "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("리프레시 토큰은 빈 값일 수 없습니다");
    }
}
