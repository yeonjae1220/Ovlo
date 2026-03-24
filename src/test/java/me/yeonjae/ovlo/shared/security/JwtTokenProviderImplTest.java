package me.yeonjae.ovlo.shared.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderImplTest {

    // 테스트용 256비트(32바이트) Base64 인코딩 시크릿
    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhTMjU2";

    private JwtTokenProviderImpl provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProviderImpl(
                TEST_SECRET,
                15L,    // access token TTL (minutes)
                10080L  // refresh token TTL (minutes = 7 days)
        );
    }

    @Nested
    @DisplayName("액세스 토큰 생성 및 파싱")
    class AccessToken {

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
            String token = provider.generateAccessToken(new MemberId(1L));

            assertThat(provider.validateAccessToken(token)).isTrue();
        }

        @Test
        @DisplayName("변조된 토큰은 유효하지 않다")
        void shouldInvalidate_tamperedToken() {
            String token = provider.generateAccessToken(new MemberId(1L));
            String tampered = token + "tampered";

            assertThat(provider.validateAccessToken(tampered)).isFalse();
        }
    }

    @Nested
    @DisplayName("issuer / audience 검증")
    class IssuerAudienceValidation {

        private SecretKey sameKey;

        @BeforeEach
        void setUpKey() {
            byte[] keyBytes = Base64.getDecoder().decode(TEST_SECRET);
            sameKey = Keys.hmacShaKeyFor(keyBytes);
        }

        @Test
        @DisplayName("issuer가 다른 토큰은 유효하지 않다")
        void shouldInvalidate_wrongIssuer() {
            String token = Jwts.builder()
                    .issuer("other-service")
                    .audience().add("ovlo-api").and()
                    .claim("memberId", 1L)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 60_000))
                    .signWith(sameKey)
                    .compact();

            assertThat(provider.validateAccessToken(token)).isFalse();
        }

        @Test
        @DisplayName("audience가 다른 토큰은 유효하지 않다")
        void shouldInvalidate_wrongAudience() {
            String token = Jwts.builder()
                    .issuer("ovlo")
                    .audience().add("other-audience").and()
                    .claim("memberId", 1L)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 60_000))
                    .signWith(sameKey)
                    .compact();

            assertThat(provider.validateAccessToken(token)).isFalse();
        }

        @Test
        @DisplayName("issuer와 audience가 모두 없는 토큰은 유효하지 않다")
        void shouldInvalidate_missingIssuerAndAudience() {
            String token = Jwts.builder()
                    .claim("memberId", 1L)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 60_000))
                    .signWith(sameKey)
                    .compact();

            assertThat(provider.validateAccessToken(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("리프레시 토큰 생성")
    class RefreshToken {

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
}
