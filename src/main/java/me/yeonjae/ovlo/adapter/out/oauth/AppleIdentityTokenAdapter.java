package me.yeonjae.ovlo.adapter.out.oauth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.Jwks;
import me.yeonjae.ovlo.application.dto.result.AppleIdentity;
import me.yeonjae.ovlo.application.port.out.oauth.AppleIdentityTokenPort;
import me.yeonjae.ovlo.domain.auth.exception.AuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.security.Key;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Apple identity token(JWT) 검증.
 *
 * <p>Apple 공개키(JWKS)는 메모리에 캐시하고, 처음 보는 kid 가 오면(키 교체) 다시 받는다.
 * 모르는 kid 를 계속 보내 Apple 키 서버를 두드리게 만들 수 없도록 재조회는 {@link #REFETCH_INTERVAL}
 * 에 한 번만 한다. 받기에 실패했을 때도 같은 간격 동안은 다시 받지 않고 바로 실패시킨다 — Apple 장애 중에
 * 모든 요청이 타임아웃까지 줄 서지 않도록. 잠금은 ReentrantLock 이다(가상 스레드가 synchronized 안의
 * 네트워크 대기로 캐리어 스레드를 붙잡지 않게). 서명 키는 RSA 공개키만 쓰므로 HS256 처럼 공개키를 비밀로 삼는 알고리즘 혼동은
 * jjwt 가 키 타입 불일치로 거절한다.
 */
@Component
public class AppleIdentityTokenAdapter implements AppleIdentityTokenPort {

    static final String ISSUER = "https://appleid.apple.com";
    private static final Duration REFETCH_INTERVAL = Duration.ofMinutes(1);
    private static final long CLOCK_SKEW_SECONDS = 60;
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);
    private static final String FAILURE = "Apple 인증에 실패했습니다. 다시 시도해 주세요";
    private static final Logger log = LoggerFactory.getLogger(AppleIdentityTokenAdapter.class);

    @FunctionalInterface
    interface JwksSource {
        String fetch();
    }

    private final JwksSource jwksSource;
    private final Set<String> clientIds;
    private final Clock clock;

    private final ReentrantLock lock = new ReentrantLock();
    private Map<String, Key> keysById = Map.of();
    private boolean loaded;
    private Instant lastRefetch = Instant.MIN;
    private Instant lastFailure = Instant.MIN;

    @Autowired
    public AppleIdentityTokenAdapter(@Value("${apple.oauth2.client-ids:}") List<String> clientIds,
                                     @Value("${apple.oauth2.jwks-uri}") String jwksUri) {
        this(httpSource(jwksUri), clientIds, Clock.systemUTC());
    }

    AppleIdentityTokenAdapter(JwksSource jwksSource, List<String> clientIds, Clock clock) {
        this.jwksSource = jwksSource;
        this.clientIds = clientIds.stream()
                .map(String::strip)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        this.clock = clock;
    }

    @Override
    public AppleIdentity verify(String identityToken) {
        if (clientIds.isEmpty()) {
            throw new AuthException("Apple 로그인이 설정되지 않았습니다");
        }
        Claims claims = parse(identityToken);
        Set<String> audience = claims.getAudience();
        String matchedClient = audience == null ? null
                : audience.stream().filter(clientIds::contains).findFirst().orElse(null);
        if (matchedClient == null) {
            throw new AuthException(FAILURE);
        }
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new AuthException(FAILURE);
        }
        return new AppleIdentity(subject, claims.get("email", String.class), claims.get("nonce", String.class),
                matchedClient);
    }

    private Claims parse(String identityToken) {
        try {
            return Jwts.parser()
                    .keyLocator(new LocatorAdapter<Key>() {
                        @Override
                        protected Key locate(JwsHeader header) {
                            return keyFor(header.getKeyId());
                        }
                    })
                    .requireIssuer(ISSUER)
                    .clock(() -> Date.from(clock.instant()))
                    .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException | ClassCastException e) {
            throw new AuthException(FAILURE, e);
        }
    }

    private Key keyFor(String keyId) {
        if (keyId == null) {
            throw new AuthException(FAILURE);
        }
        lock.lock();
        try {
            if (!loaded) {
                if (!elapsedSince(lastFailure)) {
                    throw new AuthException(FAILURE);
                }
                reload();
            } else if (!keysById.containsKey(keyId) && elapsedSince(lastRefetch)) {
                lastRefetch = clock.instant();
                reload();
            }
            Key key = keysById.get(keyId);
            if (key == null) {
                throw new AuthException(FAILURE);
            }
            return key;
        } finally {
            lock.unlock();
        }
    }

    private boolean elapsedSince(Instant last) {
        return !clock.instant().isBefore(last.plus(REFETCH_INTERVAL));
    }

    /** 실패하면 기존 키를 그대로 두고 AuthException(401). 빈 응답·깨진 JSON 도 같다. */
    private void reload() {
        Map<String, Key> parsed = new HashMap<>();
        try {
            String json = jwksSource.fetch();
            if (json == null || json.isBlank()) {
                throw new IllegalStateException("empty JWKS response");
            }
            for (Jwk<?> jwk : Jwks.setParser().build().parse(json).getKeys()) {
                Key key = jwk.toKey();
                if (jwk.getId() != null && key instanceof RSAPublicKey) {
                    parsed.put(jwk.getId(), key);
                }
            }
        } catch (RuntimeException e) {
            lastFailure = clock.instant();
            log.warn("Apple 공개키(JWKS)를 받지 못했습니다: {}", e.toString());
            throw new AuthException(FAILURE, e);
        }
        keysById = Map.copyOf(parsed);
        loaded = true;
    }

    private static JwksSource httpSource(String jwksUri) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(HTTP_TIMEOUT);
        requestFactory.setReadTimeout(HTTP_TIMEOUT);
        RestClient restClient = RestClient.builder().requestFactory(requestFactory).build();
        return () -> restClient.get().uri(jwksUri).retrieve().body(String.class);
    }
}
