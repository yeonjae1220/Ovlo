package me.yeonjae.ovlo.shared.security;

import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderImplTest {

    private JwtTokenProviderImpl provider;

    @BeforeEach
    void setUp() {
        // 테스트용 256비트(32바이트) Base64 인코딩 시크릿
        provider = new JwtTokenProviderImpl(
                "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhTMjU2",
                15L,    // access token TTL (minutes)
                10080L  // refresh token TTL (minutes = 7 days)
        );
    }

    @Test
    @DisplayName("memberId로 액세스 토큰을 생성하고 파싱할 수 있다")
    void shouldGenerateAndParseAccessToken() {
        MemberId memberId = new MemberId(42L);

        String token = provider.generateAccessToken(memberId);
        MemberId extracted = provider.extractMemberId(token);

        assertThat(extracted).isEqualTo(memberId);
    }

    @Test
    @DisplayName("생성된 액세스 토큰은 유효하다")
    void shouldValidateAccessToken() {
        MemberId memberId = new MemberId(1L);

        String token = provider.generateAccessToken(memberId);

        assertThat(provider.validateAccessToken(token)).isTrue();
    }

    @Test
    @DisplayName("변조된 토큰은 유효하지 않다")
    void shouldInvalidate_tamperedToken() {
        String token = provider.generateAccessToken(new MemberId(1L));
        String tampered = token + "tampered";

        assertThat(provider.validateAccessToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("리프레시 토큰을 생성하면 비어있지 않다")
    void shouldGenerateNonEmptyRefreshToken() {
        String refreshToken = provider.generateRefreshToken();

        assertThat(refreshToken).isNotBlank();
    }

    @Test
    @DisplayName("리프레시 토큰은 매번 다르게 생성된다")
    void shouldGenerateUniqueRefreshTokens() {
        String first = provider.generateRefreshToken();
        String second = provider.generateRefreshToken();

        assertThat(first).isNotEqualTo(second);
    }
}
