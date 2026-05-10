package me.yeonjae.ovlo.adapter.out.redis;

import me.yeonjae.ovlo.application.port.out.auth.TokenStorePort;
import me.yeonjae.ovlo.domain.auth.model.AuthSession;
import me.yeonjae.ovlo.domain.auth.model.AuthSessionId;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 다중 세션 Redis 어댑터.
 *
 * 키 구조:
 *   auth:session:{sessionId}           — 세션 Hash (기기별 독립)
 *   auth:member:sessions:{memberId}    — 해당 멤버의 sessionId Set
 *   auth:token:{refreshToken}          — refreshToken → sessionId 역인덱스
 */
@Component
public class RedisTokenAdapter implements TokenStorePort {

    private static final String SESSION_PREFIX = "auth:session:";
    private static final String MEMBER_SESSIONS_PREFIX = "auth:member:sessions:";
    private static final String TOKEN_INDEX_PREFIX = "auth:token:";

    private final RedisTemplate<String, String> redisTemplate;

    public RedisTokenAdapter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(AuthSession session) {
        String sessionKey = sessionKey(session.getId());
        String tokenIndexKey = tokenIndexKey(session.getRefreshToken());
        String memberSessionsKey = memberSessionsKey(session.getMemberId());

        Duration ttl = Duration.between(Instant.now(), session.getExpiresAt());
        if (ttl.isNegative() || ttl.isZero()) return;

        // 토큰 rotation 시 구 토큰 역인덱스 삭제 (MULTI 블록 이전에 읽어 둠)
        Map<Object, Object> existing = redisTemplate.opsForHash().entries(sessionKey);
        String oldToken = existing.isEmpty() ? null : (String) existing.get("refreshToken");
        final String oldTokenToDelete = (oldToken != null && !oldToken.equals(session.getRefreshToken()))
                ? oldToken : null;

        Map<String, String> fields = new HashMap<>();
        fields.put("sessionId", session.getId().value());
        fields.put("memberId", String.valueOf(session.getMemberId().value()));
        fields.put("refreshToken", session.getRefreshToken());
        fields.put("expiresAt", String.valueOf(session.getExpiresAt().toEpochMilli()));
        fields.put("revoked", String.valueOf(session.isRevoked()));

        // MULTI/EXEC: 구 토큰 삭제 + 세션 저장 + 멤버 세션 Set 등록을 원자적으로 실행
        redisTemplate.execute(new SessionCallback<Void>() {
            @Override
            @SuppressWarnings("unchecked")
            public <K, V> Void execute(RedisOperations<K, V> ops) {
                RedisOperations<String, String> operations = (RedisOperations<String, String>) ops;
                operations.multi();
                if (oldTokenToDelete != null) {
                    operations.delete(tokenIndexKey(oldTokenToDelete));
                }
                operations.opsForHash().putAll(sessionKey, fields);
                operations.expire(sessionKey, ttl);
                operations.opsForSet().add(memberSessionsKey, session.getId().value());
                operations.exec();
                return null;
            }
        });

        // 멤버 세션 Set TTL은 세션보다 하루 길게 유지 (stale entry 자동 정리용)
        redisTemplate.expire(memberSessionsKey, ttl.plusDays(1));

        // 토큰 역인덱스: refreshToken → sessionId
        redisTemplate.opsForValue().set(tokenIndexKey, session.getId().value(), ttl);
    }

    @Override
    public Optional<AuthSession> findByRefreshToken(String refreshToken) {
        String sessionId = redisTemplate.opsForValue().get(tokenIndexKey(refreshToken));
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
        String tokenIndexKey = tokenIndexKey(refreshToken);
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
                    String token = (String) fields.get("refreshToken");
                    if (token != null) redisTemplate.delete(tokenIndexKey(token));
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

    private String tokenIndexKey(String token) {
        return TOKEN_INDEX_PREFIX + token;
    }
}
