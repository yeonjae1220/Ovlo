package me.yeonjae.ovlo.adapter.out.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jsonwebtoken.Jwts;
import me.yeonjae.ovlo.application.dto.result.AppleTokens;
import me.yeonjae.ovlo.application.port.out.oauth.AppleTokenRevocationPort;
import me.yeonjae.ovlo.domain.auth.exception.AuthException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * Apple 토큰 교환·폐기.
 *
 * <p>client secret 은 Apple 개발자 계정의 Sign in with Apple 키(.p8, P-256)로 서명한 ES256 JWT 다
 * (iss=Team ID, sub=client ID, aud=https://appleid.apple.com). 요청마다 5분짜리로 새로 만든다.
 * 교환 응답의 id_token 은 Apple 토큰 엔드포인트와 TLS 로 직접 받은 것이라 서명 검증 없이 sub 만 읽는다.
 */
@Component
public class AppleTokenRevocationAdapter implements AppleTokenRevocationPort {

    private static final String AUDIENCE = "https://appleid.apple.com";
    private static final Duration CLIENT_SECRET_TTL = Duration.ofMinutes(5);
    private static final String UNAVAILABLE = "Apple 계정 연결을 해제하지 못했습니다. 잠시 후 다시 시도해 주세요";
    private static final String REAUTH = "Apple 인증이 만료됐습니다. 다시 시도해 주세요";
    private static final Logger log = LoggerFactory.getLogger(AppleTokenRevocationAdapter.class);

    private final String teamId;
    private final String keyId;
    private final PrivateKey signingKey;
    private final URI tokenUri;
    private final URI revokeUri;
    private final Clock clock;
    private final RestClient restClient;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    public AppleTokenRevocationAdapter(@Value("${apple.oauth2.team-id:}") String teamId,
                                       @Value("${apple.oauth2.key-id:}") String keyId,
                                       @Value("${apple.oauth2.private-key:}") String privateKeyPem,
                                       @Value("${apple.oauth2.token-uri}") URI tokenUri,
                                       @Value("${apple.oauth2.revoke-uri}") URI revokeUri) {
        this(teamId, keyId, privateKeyPem, tokenUri, revokeUri, Duration.ofSeconds(5), Clock.systemUTC());
    }

    AppleTokenRevocationAdapter(String teamId, String keyId, String privateKeyPem,
                                URI tokenUri, URI revokeUri, Duration timeout, Clock clock) {
        this.teamId = teamId == null ? "" : teamId.strip();
        this.keyId = keyId == null ? "" : keyId.strip();
        this.signingKey = parsePrivateKey(privateKeyPem);
        this.tokenUri = tokenUri;
        this.revokeUri = revokeUri;
        this.clock = clock;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public AppleTokens exchange(String authorizationCode, String clientId, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret(clientId));
        form.add("code", authorizationCode);
        form.add("grant_type", "authorization_code");
        if (redirectUri != null && !redirectUri.isBlank()) {
            form.add("redirect_uri", redirectUri);
        }
        TokenResponse response;
        try {
            response = restClient.post().uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                log.info("Apple 이 authorization code 를 거절했습니다: status={}, error={}",
                        e.getStatusCode().value(), appleErrorCode(e));
                throw new AuthException(AuthException.ErrorType.UNAUTHORIZED, REAUTH);
            }
            throw unavailable("token exchange", e);
        } catch (RestClientException e) {
            throw unavailable("token exchange", e);
        }
        if (response == null || response.idToken() == null) {
            throw unavailable("token exchange", new IllegalStateException("no id_token in response"));
        }
        return new AppleTokens(subjectOf(response.idToken()), response.refreshToken(), response.accessToken());
    }

    @Override
    public void revoke(String clientId, String token, TokenType type) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret(clientId));
        form.add("token", token);
        form.add("token_type_hint", type == TokenType.REFRESH_TOKEN ? "refresh_token" : "access_token");
        try {
            restClient.post().uri(revokeUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw unavailable("revoke", e);
        }
    }

    private String clientSecret(String clientId) {
        if (signingKey == null || teamId.isEmpty() || keyId.isEmpty()) {
            log.error("Apple 토큰 폐기 설정(APPLE_TEAM_ID·APPLE_KEY_ID·APPLE_PRIVATE_KEY)이 없어 Apple 회원이 탈퇴할 수 없습니다");
            throw new AuthException(AuthException.ErrorType.UNAVAILABLE, UNAVAILABLE);
        }
        Instant now = clock.instant();
        return Jwts.builder()
                .header().keyId(keyId).and()
                .issuer(teamId)
                .subject(clientId)
                .audience().add(AUDIENCE).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(CLIENT_SECRET_TTL)))
                .signWith(signingKey, Jwts.SIG.ES256)
                .compact();
    }

    private String subjectOf(String idToken) {
        String[] parts = idToken.split("\\.");
        try {
            JsonNode payload = JSON.readTree(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode sub = payload.get("sub");
            String subject = sub == null || !sub.isString() ? null : sub.asString();
            if (subject == null || subject.isBlank()) {
                throw new IllegalStateException("id_token without sub");
            }
            return subject;
        } catch (Exception e) {
            throw unavailable("token exchange", e);
        }
    }

    // 응답 본문은 로그에 남기지 않는다 — 상태 코드·예외 종류·Apple 의 error 코드(invalid_grant 등)만.
    private static AuthException unavailable(String step, Exception cause) {
        if (cause instanceof RestClientResponseException e) {
            log.warn("Apple {} 실패: status={}, error={}", step, e.getStatusCode().value(), appleErrorCode(e));
        } else {
            log.warn("Apple {} 실패: {}", step, cause.getClass().getSimpleName());
        }
        return new AuthException(AuthException.ErrorType.UNAVAILABLE, UNAVAILABLE);
    }

    private static String appleErrorCode(RestClientResponseException e) {
        try {
            JsonNode error = JSON.readTree(e.getResponseBodyAsByteArray()).get("error");
            return error != null && error.isString() && error.asString().matches("[a-z_]{1,64}") ? error.asString() : "-";
        } catch (Exception ignored) {
            return "-";
        }
    }

    /** .p8(PKCS#8 PEM) 문자열. 비었거나 깨졌으면 null — 탈퇴 시점에 503 과 오류 로그로 드러난다. */
    private static PrivateKey parsePrivateKey(String pem) {
        if (pem == null || pem.isBlank()) {
            return null;
        }
        try {
            String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("\\n", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(base64.getBytes(StandardCharsets.US_ASCII));
            return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            log.error("APPLE_PRIVATE_KEY 를 읽지 못했습니다: {}", e.getClass().getSimpleName());
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("id_token") String idToken
    ) {}
}
