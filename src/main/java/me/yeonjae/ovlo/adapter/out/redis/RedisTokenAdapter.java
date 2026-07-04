package me.yeonjae.ovlo.adapter.out.redis;

import me.yeonjae.ovlo.application.port.out.auth.TokenStorePort;
import me.yeonjae.ovlo.domain.auth.model.AuthSession;
import me.yeonjae.ovlo.domain.auth.model.AuthSessionId;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.shared.security.TokenHashUtil;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 다중 세션 Redis 어댑터.
 *
 * 키 구조:
 *   auth:session:{sessionId}           — 세션 Hash (기기별 독립, refreshToken 필드는 SHA-256 해시)
 *   auth:member:sessions:{memberId}    — 해당 멤버의 sessionId Set
 *   auth:token:{sha256(refreshToken)}  — refreshToken 해시 → sessionId 역인덱스
 *
 * 보안: refresh token은 고엔트로피이므로 SHA-256 단방향 해시만 저장한다. Redis가
 * 유출되어도 원문 토큰을 복원할 수 없어 세션 탈취를 막는다 (GLOBAL-PIT-001).
 */
@Component
public class RedisTokenAdapter implements TokenStorePort {

    private static final String SESSION_PREFIX = "auth:session:";
    private static final String MEMBER_SESSIONS_PREFIX = "auth:member:sessions:";
    private static final String TOKEN_INDEX_PREFIX = "auth:token:";

    /** WATCH/MULTI/EXEC CAS가 동시 rotation 경합으로 abort될 때 재시도하는 최대 횟수. */
    private static final int MAX_SAVE_ATTEMPTS = 3;

    private final RedisTemplate<String, String> redisTemplate;

    public RedisTokenAdapter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(AuthSession session) {
        String hashedToken = TokenHashUtil.sha256(session.getRefreshToken());
        String sessionKey = sessionKey(session.getId());
        String tokenIndexKey = tokenIndexKey(hashedToken);
        String memberSessionsKey = memberSessionsKey(session.getMemberId());

        Duration ttl = Duration.between(Instant.now(), session.getExpiresAt());
        if (ttl.isNegative() || ttl.isZero()) return;

        Map<String, String> fields = new HashMap<>();
        fields.put("sessionId", session.getId().value());
        fields.put("memberId", String.valueOf(session.getMemberId().value()));
        fields.put("refreshToken", hashedToken);
        fields.put("expiresAt", String.valueOf(session.getExpiresAt().toEpochMilli()));
        fields.put("revoked", String.valueOf(session.isRevoked()));

        // WATCH/MULTI/EXEC 낙관적 락(CAS):
        //   구 토큰 역인덱스 삭제 여부는 "현재 저장된 refreshToken"에 의존하는 read-modify-write다.
        //   이 읽기를 MULTI 바깥에서 하면 동시 rotation 시 두 요청이 같은 구 토큰을 보고 각자
        //   새 토큰을 저장해, 한 세션에 유효한 refresh 토큰이 둘 남는다(single-use rotation 붕괴).
        //   따라서 sessionKey를 WATCH한 뒤 그 안에서 구 토큰을 읽고 트랜잭션을 실행한다. 다른
        //   요청이 먼저 sessionKey를 바꾸면 exec()가 null(abort)을 반환하므로 최신 값 기준으로
        //   재시도해, 경합 후에도 유효 토큰이 정확히 하나만 남도록 보장한다.
        for (int attempt = 0; attempt < MAX_SAVE_ATTEMPTS; attempt++) {
            Boolean committed = redisTemplate.execute(new SessionCallback<Boolean>() {
                @Override
                @SuppressWarnings("unchecked")
                public <K, V> Boolean execute(RedisOperations<K, V> ops) {
                    RedisOperations<String, String> operations = (RedisOperations<String, String>) ops;
                    operations.watch(sessionKey);

                    // 세션 Hash에는 이미 해시값이 저장돼 있으므로 재해시 없이 그대로 비교/삭제한다.
                    Object current = operations.opsForHash().get(sessionKey, "refreshToken");
                    String oldHashedToken = (current instanceof String s) ? s : null;
                    String oldTokenIndexKeyToDelete =
                            (oldHashedToken != null && !oldHashedToken.equals(hashedToken))
                                    ? tokenIndexKey(oldHashedToken) : null;

                    operations.multi();
                    if (oldTokenIndexKeyToDelete != null) {
                        operations.delete(oldTokenIndexKeyToDelete);
                    }
                    operations.opsForHash().putAll(sessionKey, fields);
                    operations.expire(sessionKey, ttl);
                    operations.opsForSet().add(memberSessionsKey, session.getId().value());
                    operations.expire(memberSessionsKey, ttl.plusDays(1));
                    operations.opsForValue().set(tokenIndexKey, session.getId().value(), ttl);

                    // exec()가 null이면 WATCH한 sessionKey가 변경돼 트랜잭션이 취소된 것 → 재시도
                    List<Object> results = operations.exec();
                    return results != null;
                }
            });

            if (Boolean.TRUE.equals(committed)) return;
        }
        // WATCH CAS가 MAX_SAVE_ATTEMPTS 회 연속 abort — 동시 rotation 경합(낙관적 동시성 제어 실패).
        // GlobalExceptionHandler가 409(다시 시도)로 매핑한다. 500 대신 재시도 가능 신호를 준다.
        throw new OptimisticLockingFailureException(
                "세션 저장이 동시 갱신 경합으로 실패했습니다: " + session.getId().value());
    }

    @Override
    public Optional<AuthSession> findByRefreshToken(String refreshToken) {
        String sessionId = redisTemplate.opsForValue().get(tokenIndexKey(TokenHashUtil.sha256(refreshToken)));
        if (sessionId == null) return Optional.empty();

        Map<Object, Object> fields = redisTemplate.opsForHash().entries(sessionKey(new AuthSessionId(sessionId)));
        if (fields.isEmpty()) return Optional.empty();
        return Optional.of(toAuthSession(fields));
    }

    @Override
    public Optional<AuthSession> findByMemberId(MemberId memberId) {
        Set<String> sessionIds = redisTemplate.opsForSet().members(memberSessionsKey(memberId));
        if (sessionIds == null || sessionIds.isEmpty()) return Optional.empty();

        for (String sessionId : sessionIds) {
            Map<Object, Object> fields = redisTemplate.opsForHash().entries(sessionKey(new AuthSessionId(sessionId)));
            if (!fields.isEmpty()) return Optional.of(toAuthSession(fields));
        }
        return Optional.empty();
    }

    @Override
    public void deleteByRefreshToken(String refreshToken) {
        String tokenIndexKey = tokenIndexKey(TokenHashUtil.sha256(refreshToken));
        String sessionId = redisTemplate.opsForValue().get(tokenIndexKey);
        if (sessionId == null) return;

        String sessionKey = sessionKey(new AuthSessionId(sessionId));
        Map<Object, Object> fields = redisTemplate.opsForHash().entries(sessionKey);

        if (!fields.isEmpty()) {
            String memberIdStr = (String) fields.get("memberId");
            if (memberIdStr != null) {
                redisTemplate.opsForSet().remove(
                        memberSessionsKey(new MemberId(Long.valueOf(memberIdStr))), sessionId);
            }
        }

        redisTemplate.delete(tokenIndexKey);
        redisTemplate.delete(sessionKey);
    }

    @Override
    public void delete(MemberId memberId) {
        String memberSessionsKey = memberSessionsKey(memberId);
        Set<String> sessionIds = redisTemplate.opsForSet().members(memberSessionsKey);

        if (sessionIds != null) {
            for (String sessionId : sessionIds) {
                String sessionKey = sessionKey(new AuthSessionId(sessionId));
                Map<Object, Object> fields = redisTemplate.opsForHash().entries(sessionKey);
                if (!fields.isEmpty()) {
                    // 필드에는 이미 해시값이 저장되어 있으므로 그대로 역인덱스 키를 구성한다
                    String hashedToken = (String) fields.get("refreshToken");
                    if (hashedToken != null) redisTemplate.delete(tokenIndexKey(hashedToken));
                    redisTemplate.delete(sessionKey);
                }
            }
        }
        redisTemplate.delete(memberSessionsKey);
    }

    private AuthSession toAuthSession(Map<Object, Object> fields) {
        String sessionIdStr = (String) fields.get("sessionId");
        String memberIdStr  = (String) fields.get("memberId");
        String refreshToken = (String) fields.get("refreshToken");
        String expiresAtStr = (String) fields.get("expiresAt");
        String revokedStr   = (String) fields.get("revoked");

        if (sessionIdStr == null || memberIdStr == null || refreshToken == null || expiresAtStr == null) {
            throw new IllegalStateException("Redis 세션 데이터가 손상되었습니다");
        }

        return AuthSession.restore(
                new AuthSessionId(sessionIdStr),
                new MemberId(Long.valueOf(memberIdStr)),
                refreshToken,
                Instant.ofEpochMilli(Long.parseLong(expiresAtStr)),
                Boolean.parseBoolean(revokedStr)
        );
    }

    private String sessionKey(AuthSessionId sessionId) {
        return SESSION_PREFIX + sessionId.value();
    }

    private String memberSessionsKey(MemberId memberId) {
        return MEMBER_SESSIONS_PREFIX + memberId.value();
    }

    /**
     * 역인덱스 키를 만든다. 인자는 <b>이미 SHA-256으로 해시된</b> 토큰값이어야 한다.
     * 원문 토큰이 들어오는 진입점(save/find/delete)에서 {@link TokenHashUtil#sha256}로
     * 변환한 뒤 호출한다.
     */
    private String tokenIndexKey(String hashedToken) {
        return TOKEN_INDEX_PREFIX + hashedToken;
    }
}
