package me.yeonjae.ovlo.adapter.out.oauth;

import io.jsonwebtoken.Jwts;
import me.yeonjae.ovlo.application.dto.result.AppleIdentity;
import me.yeonjae.ovlo.domain.auth.exception.AuthException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppleIdentityTokenAdapterTest {

    private static final String ISSUER = "https://appleid.apple.com";
    private static final String WEB_CLIENT = "com.mungji.ovlo.web";
    private static final String APP_CLIENT = "com.mungji.ovlo";
    private static final Instant NOW = Instant.parse("2026-11-26T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static KeyPair appleKey;
    private static KeyPair otherKey;

    @BeforeAll
    static void keys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        appleKey = generator.generateKeyPair();
        otherKey = generator.generateKeyPair();
    }

    @Test
    @DisplayName("Apple 공개키로 서명되고 iss·aud·exp 가 맞는 토큰에서 sub·email·nonce 를 꺼낸다")
    void validToken_returnsIdentity() {
        AppleIdentityTokenAdapter sut = adapter(() -> jwks("kid-1", appleKey));

        AppleIdentity identity = sut.verify(token("kid-1", appleKey, ISSUER, APP_CLIENT, NOW.plusSeconds(300)));

        assertThat(identity.subject()).isEqualTo("apple-sub");
        assertThat(identity.email()).isEqualTo("hidden@privaterelay.appleid.com");
        assertThat(identity.nonce()).isEqualTo("hashed-nonce");
    }

    @Test
    @DisplayName("웹(Services ID)과 앱(번들 ID) 두 audience 를 모두 받아들인다")
    void acceptsEveryConfiguredAudience() {
        AppleIdentityTokenAdapter sut = adapter(() -> jwks("kid-1", appleKey));

        assertThat(sut.verify(token("kid-1", appleKey, ISSUER, WEB_CLIENT, NOW.plusSeconds(300))).subject())
                .isEqualTo("apple-sub");
    }

    @Test
    @DisplayName("다른 앱용으로 발급된 토큰(aud 불일치)은 거절한다")
    void foreignAudience_rejected() {
        AppleIdentityTokenAdapter sut = adapter(() -> jwks("kid-1", appleKey));

        assertThatThrownBy(() -> sut.verify(token("kid-1", appleKey, ISSUER, "com.attacker.app", NOW.plusSeconds(300))))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("발급자가 Apple 이 아니면 거절한다")
    void wrongIssuer_rejected() {
        AppleIdentityTokenAdapter sut = adapter(() -> jwks("kid-1", appleKey));

        assertThatThrownBy(() -> sut.verify(token("kid-1", appleKey, "https://evil.example", APP_CLIENT, NOW.plusSeconds(300))))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("만료된 토큰은 거절한다")
    void expired_rejected() {
        AppleIdentityTokenAdapter sut = adapter(() -> jwks("kid-1", appleKey));

        assertThatThrownBy(() -> sut.verify(token("kid-1", appleKey, ISSUER, APP_CLIENT, NOW.minusSeconds(120))))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("Apple 키가 아닌 키로 서명된 토큰은 kid 가 같아도 거절한다")
    void forgedSignature_rejected() {
        AppleIdentityTokenAdapter sut = adapter(() -> jwks("kid-1", appleKey));

        assertThatThrownBy(() -> sut.verify(token("kid-1", otherKey, ISSUER, APP_CLIENT, NOW.plusSeconds(300))))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("대칭키(HS256) 서명 토큰은 거절한다 — 공개키를 HMAC 비밀로 쓰는 알고리즘 혼동 공격 방지")
    void hmacSignedToken_rejected() {
        AppleIdentityTokenAdapter sut = adapter(() -> jwks("kid-1", appleKey));
        String hmac = Jwts.builder()
                .header().keyId("kid-1").and()
                .issuer(ISSUER).audience().add(APP_CLIENT).and().subject("apple-sub")
                .expiration(Date.from(NOW.plusSeconds(300)))
                .signWith(new SecretKeySpec("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .compact();

        assertThatThrownBy(() -> sut.verify(hmac)).isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("서명 없는 토큰(alg=none)은 거절한다")
    void unsignedToken_rejected() {
        AppleIdentityTokenAdapter sut = adapter(() -> jwks("kid-1", appleKey));
        String unsigned = Jwts.builder()
                .issuer(ISSUER).audience().add(APP_CLIENT).and().subject("apple-sub")
                .expiration(Date.from(NOW.plusSeconds(300)))
                .compact();

        assertThatThrownBy(() -> sut.verify(unsigned)).isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("Apple 이 키를 교체해 처음 보는 kid 가 오면 키 목록을 다시 받아 검증한다")
    void unknownKid_refetchesKeys() {
        AtomicInteger fetches = new AtomicInteger();
        AppleIdentityTokenAdapter sut = adapter(() ->
                fetches.incrementAndGet() == 1 ? jwks("kid-old", otherKey) : jwks("kid-new", appleKey));

        sut.verify(token("kid-old", otherKey, ISSUER, APP_CLIENT, NOW.plusSeconds(300)));
        AppleIdentity identity = sut.verify(token("kid-new", appleKey, ISSUER, APP_CLIENT, NOW.plusSeconds(300)));

        assertThat(identity.subject()).isEqualTo("apple-sub");
        assertThat(fetches).hasValue(2);
    }

    @Test
    @DisplayName("이미 아는 kid 로 오는 토큰은 키 목록을 다시 받지 않는다")
    void knownKid_usesCache() {
        AtomicInteger fetches = new AtomicInteger();
        AppleIdentityTokenAdapter sut = adapter(() -> {
            fetches.incrementAndGet();
            return jwks("kid-1", appleKey);
        });

        sut.verify(token("kid-1", appleKey, ISSUER, APP_CLIENT, NOW.plusSeconds(300)));
        sut.verify(token("kid-1", appleKey, ISSUER, APP_CLIENT, NOW.plusSeconds(300)));

        assertThat(fetches).hasValue(1);
    }

    @Test
    @DisplayName("모르는 kid 로 키 목록을 계속 다시 받게 만들 수 없다 — 재조회는 간격을 둔다")
    void unknownKid_refetchIsThrottled() {
        AtomicInteger fetches = new AtomicInteger();
        AppleIdentityTokenAdapter sut = adapter(() -> {
            fetches.incrementAndGet();
            return jwks("kid-1", appleKey);
        });
        sut.verify(token("kid-1", appleKey, ISSUER, APP_CLIENT, NOW.plusSeconds(300)));

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> sut.verify(token("kid-unknown", otherKey, ISSUER, APP_CLIENT, NOW.plusSeconds(300))))
                    .isInstanceOf(AuthException.class);
        }

        assertThat(fetches).hasValue(2);
    }

    @Test
    @DisplayName("Apple 키 서버에 못 붙으면 인증 실패로 응답한다(500 이 아니라)")
    void jwksUnavailable_authFailure() {
        AppleIdentityTokenAdapter sut = adapter(() -> {
            throw new IllegalStateException("network down");
        });

        assertThatThrownBy(() -> sut.verify(token("kid-1", appleKey, ISSUER, APP_CLIENT, NOW.plusSeconds(300))))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("Apple 키 서버 장애 중에는 요청마다 다시 받지 않고 간격을 두고 재시도한다")
    void jwksFailure_isThrottled() {
        AtomicInteger fetches = new AtomicInteger();
        MutableClock clock = new MutableClock(NOW);
        AppleIdentityTokenAdapter sut = new AppleIdentityTokenAdapter(() -> {
            if (fetches.incrementAndGet() == 1) throw new IllegalStateException("network down");
            return jwks("kid-1", appleKey);
        }, List.of(APP_CLIENT), clock);
        String valid = token("kid-1", appleKey, ISSUER, APP_CLIENT, NOW.plusSeconds(600));

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> sut.verify(valid)).isInstanceOf(AuthException.class);
        }
        assertThat(fetches).hasValue(1);

        clock.advance(Duration.ofMinutes(1));
        assertThat(sut.verify(valid).subject()).isEqualTo("apple-sub");
        assertThat(fetches).hasValue(2);
    }

    @Test
    @DisplayName("Apple 이 빈 응답이나 깨진 키 목록을 주면 500 이 아니라 인증 실패")
    void malformedJwks_authFailure() {
        for (String body : new String[]{null, "", "not json", "{\"keys\":\"nope\"}"}) {
            AppleIdentityTokenAdapter sut = adapter(() -> body);
            assertThatThrownBy(() -> sut.verify(token("kid-1", appleKey, ISSUER, APP_CLIENT, NOW.plusSeconds(300))))
                    .as("body=%s", body)
                    .isInstanceOf(AuthException.class);
        }
    }

    @Test
    @DisplayName("client ID 가 설정되지 않았으면 어떤 토큰도 받지 않는다")
    void notConfigured_rejectsEverything() {
        AppleIdentityTokenAdapter sut = new AppleIdentityTokenAdapter(() -> jwks("kid-1", appleKey), List.of(), CLOCK);

        assertThatThrownBy(() -> sut.verify(token("kid-1", appleKey, ISSUER, APP_CLIENT, NOW.plusSeconds(300))))
                .isInstanceOf(AuthException.class);
    }

    private static AppleIdentityTokenAdapter adapter(AppleIdentityTokenAdapter.JwksSource source) {
        return new AppleIdentityTokenAdapter(source, List.of(WEB_CLIENT, APP_CLIENT), CLOCK);
    }

    private static String token(String kid, KeyPair key, String issuer, String audience, Instant expiresAt) {
        return Jwts.builder()
                .header().keyId(kid).and()
                .issuer(issuer)
                .audience().add(audience).and()
                .subject("apple-sub")
                .issuedAt(Date.from(expiresAt.minus(Duration.ofMinutes(10))))
                .expiration(Date.from(expiresAt))
                .claim("email", "hidden@privaterelay.appleid.com")
                .claim("email_verified", "true")
                .claim("nonce", "hashed-nonce")
                .signWith(key.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    private static String jwks(String kid, KeyPair key) {
        RSAPublicKey publicKey = (RSAPublicKey) key.getPublic();
        return """
                {"keys":[{"kty":"RSA","kid":"%s","use":"sig","alg":"RS256","n":"%s","e":"%s"}]}
                """.formatted(kid, base64Url(publicKey.getModulus()), base64Url(publicKey.getPublicExponent()));
    }

    private static String base64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) { this.now = now; }

        void advance(Duration duration) { now = now.plus(duration); }

        @Override public Instant instant() { return now; }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }
}
