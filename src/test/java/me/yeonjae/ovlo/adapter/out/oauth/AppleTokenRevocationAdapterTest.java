package me.yeonjae.ovlo.adapter.out.oauth;

import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import me.yeonjae.ovlo.application.dto.result.AppleTokens;
import me.yeonjae.ovlo.application.port.out.oauth.AppleTokenRevocationPort.TokenType;
import me.yeonjae.ovlo.domain.auth.exception.AuthException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppleTokenRevocationAdapterTest {

    private static final String TEAM_ID = "TEAM123456";
    private static final String KEY_ID = "KEY1234567";
    private static final String CLIENT = "com.mungji.ovlo.web";
    private static final Instant NOW = Instant.parse("2026-11-26T00:00:00Z");

    private static KeyPair signingKey;
    private static String privateKeyPem;

    private HttpServer server;
    private final List<Map<String, String>> tokenRequests = new CopyOnWriteArrayList<>();
    private final List<Map<String, String>> revokeRequests = new CopyOnWriteArrayList<>();
    private final AtomicInteger tokenStatus = new AtomicInteger(200);
    private final AtomicReference<String> tokenBody = new AtomicReference<>();
    private final AtomicInteger revokeStatus = new AtomicInteger(200);

    @BeforeAll
    static void keys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        signingKey = generator.generateKeyPair();
        privateKeyPem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                        .encodeToString(signingKey.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
    }

    @BeforeEach
    void startServer() throws IOException {
        tokenBody.set(tokenResponse("apple-sub", "refresh-token"));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/auth/token", exchange -> {
            tokenRequests.add(form(exchange.getRequestBody().readAllBytes()));
            byte[] body = (tokenStatus.get() == 200 ? tokenBody.get() : "{\"error\":\"invalid_grant\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(tokenStatus.get(), body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/auth/revoke", exchange -> {
            revokeRequests.add(form(exchange.getRequestBody().readAllBytes()));
            exchange.sendResponseHeaders(revokeStatus.get(), -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    @DisplayName("authorization code 를 ES256 client secret 으로 교환하고 refresh token 과 Apple 사용자 식별자를 돌려준다")
    void exchange_sendsSignedClientSecret() {
        AppleTokens tokens = adapter(privateKeyPem).exchange("auth-code", CLIENT, "https://ovlo.test/login");

        assertThat(tokens.subject()).isEqualTo("apple-sub");
        assertThat(tokens.refreshToken()).isEqualTo("refresh-token");
        Map<String, String> request = tokenRequests.getFirst();
        assertThat(request).containsEntry("grant_type", "authorization_code")
                .containsEntry("code", "auth-code")
                .containsEntry("client_id", CLIENT)
                .containsEntry("redirect_uri", "https://ovlo.test/login");

        Jws<Claims> secret = Jwts.parser()
                .verifyWith(signingKey.getPublic())
                .clock(() -> java.util.Date.from(NOW))
                .build()
                .parseSignedClaims(request.get("client_secret"));
        assertThat(secret.getHeader().getKeyId()).isEqualTo(KEY_ID);
        assertThat(secret.getHeader().getAlgorithm()).isEqualTo("ES256");
        assertThat(secret.getPayload().getIssuer()).isEqualTo(TEAM_ID);
        assertThat(secret.getPayload().getSubject()).isEqualTo(CLIENT);
        assertThat(secret.getPayload().getAudience()).containsExactly("https://appleid.apple.com");
        assertThat(Duration.between(secret.getPayload().getIssuedAt().toInstant(),
                secret.getPayload().getExpiration().toInstant())).isPositive().isLessThanOrEqualTo(Duration.ofHours(1));
    }

    @Test
    @DisplayName("앱(네이티브)에서 받은 code 는 redirect_uri 없이 교환한다")
    void exchange_withoutRedirectUri() {
        adapter(privateKeyPem).exchange("auth-code", "com.mungji.ovlo", null);

        assertThat(tokenRequests.getFirst()).doesNotContainKey("redirect_uri").containsEntry("client_id", "com.mungji.ovlo");
    }

    @Test
    @DisplayName("Apple 이 code 를 거절하면(만료·재사용) 다시 인증하라는 401")
    void exchange_invalidGrant_unauthorized() {
        tokenStatus.set(400);

        assertThatThrownBy(() -> adapter(privateKeyPem).exchange("used-code", CLIENT, null))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorType())
                .isEqualTo(AuthException.ErrorType.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Apple 서버 오류는 503 — 잠시 뒤 다시 시도할 수 있다")
    void exchange_serverError_unavailable() {
        tokenStatus.set(500);

        assertThatThrownBy(() -> adapter(privateKeyPem).exchange("auth-code", CLIENT, null))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorType())
                .isEqualTo(AuthException.ErrorType.UNAVAILABLE);
    }

    @Test
    @DisplayName("교환 응답에 id_token 이 없으면 누구의 토큰인지 알 수 없어 실패로 본다")
    void exchange_withoutIdToken_unavailable() {
        tokenBody.set("{\"access_token\":\"a\",\"refresh_token\":\"r\"}");

        assertThatThrownBy(() -> adapter(privateKeyPem).exchange("auth-code", CLIENT, null))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorType())
                .isEqualTo(AuthException.ErrorType.UNAVAILABLE);
    }

    @Test
    @DisplayName("폐기 요청에 토큰과 종류 힌트, 서명된 client secret 을 보낸다")
    void revoke_postsTokenWithHint() {
        adapter(privateKeyPem).revoke(CLIENT, "refresh-token", TokenType.REFRESH_TOKEN);

        assertThat(revokeRequests.getFirst())
                .containsEntry("client_id", CLIENT)
                .containsEntry("token", "refresh-token")
                .containsEntry("token_type_hint", "refresh_token")
                .containsKey("client_secret");
    }

    @Test
    @DisplayName("폐기가 실패하면 503")
    void revoke_failure_unavailable() {
        revokeStatus.set(500);

        assertThatThrownBy(() -> adapter(privateKeyPem).revoke(CLIENT, "refresh-token", TokenType.REFRESH_TOKEN))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorType())
                .isEqualTo(AuthException.ErrorType.UNAVAILABLE);
    }

    @Test
    @DisplayName("Apple 서버에 닿지 않으면 503")
    void unreachable_unavailable() {
        server.stop(0);

        assertThatThrownBy(() -> adapter(privateKeyPem).exchange("auth-code", CLIENT, null))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorType())
                .isEqualTo(AuthException.ErrorType.UNAVAILABLE);
    }

    @Test
    @DisplayName("서명 키가 설정되지 않았으면 Apple 에 요청하지 않고 503")
    void notConfigured_unavailableWithoutCalls() {
        for (String key : new String[]{"", "not a pem"}) {
            assertThatThrownBy(() -> adapter(key).exchange("auth-code", CLIENT, null))
                    .isInstanceOf(AuthException.class)
                    .extracting(e -> ((AuthException) e).getErrorType())
                    .isEqualTo(AuthException.ErrorType.UNAVAILABLE);
        }
        assertThat(tokenRequests).isEmpty();
    }

    private AppleTokenRevocationAdapter adapter(String pem) {
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        return new AppleTokenRevocationAdapter(TEAM_ID, KEY_ID, pem,
                URI.create(base + "/auth/token"), URI.create(base + "/auth/revoke"),
                Duration.ofSeconds(2), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static String tokenResponse(String subject, String refreshToken) {
        String header = base64Url("{\"alg\":\"RS256\",\"kid\":\"k\"}");
        String payload = base64Url("{\"iss\":\"https://appleid.apple.com\",\"sub\":\"" + subject + "\"}");
        return "{\"access_token\":\"access-token\",\"token_type\":\"Bearer\",\"expires_in\":3600,"
                + "\"refresh_token\":\"" + refreshToken + "\",\"id_token\":\"" + header + "." + payload + ".sig\"}";
    }

    private static String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String, String> form(byte[] body) {
        Map<String, String> values = new HashMap<>();
        for (String pair : new String(body, StandardCharsets.UTF_8).split("&")) {
            if (pair.isEmpty()) continue;
            String[] kv = pair.split("=", 2);
            values.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return values;
    }
}
