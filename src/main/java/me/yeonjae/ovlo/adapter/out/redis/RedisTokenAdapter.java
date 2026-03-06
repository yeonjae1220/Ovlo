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

@Component
public class RedisTokenAdapter implements TokenStorePort {

    private static final String SESSION_BY_MEMBER_PREFIX = "auth:session:member:";
    private static final String TOKEN_INDEX_PREFIX = "auth:token:";

    private final RedisTemplate<String, String> redisTemplate;

    public RedisTokenAdapter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(AuthSession session) {
        String memberKey = memberKey(session.getMemberId());
        String tokenIndexKey = tokenIndexKey(session.getRefreshToken());

        Duration ttl = Duration.between(Instant.now(), session.getExpiresAt());
        if (ttl.isNegative() || ttl.isZero()) return;

        Map<String, String> fields = new HashMap<>();
        fields.put("sessionId", session.getId().value());
        fields.put("memberId", String.valueOf(session.getMemberId().value()));
        fields.put("refreshToken", session.getRefreshToken());
        fields.put("expiresAt", String.valueOf(session.getExpiresAt().toEpochMilli()));
        fields.put("revoked", String.valueOf(session.isRevoked()));

        // MULTI/EXEC: putAll + expire를 원자적으로 실행 (expire 실패 시 TTL 누락 방지)
        redisTemplate.execute(new SessionCallback<Void>() {
            @Override
            @SuppressWarnings("unchecked")
            public <K, V> Void execute(RedisOperations<K, V> ops) {
                RedisOperations<String, String> operations = (RedisOperations<String, String>) ops;
                operations.multi();
                operations.opsForHash().putAll(memberKey, fields);
                operations.expire(memberKey, ttl);
                operations.exec();
                return null;
            }
        });

        // token → memberId 역인덱스 (빠른 조회용, SET + TTL은 단일 명령으로 원자적)
        redisTemplate.opsForValue().set(tokenIndexKey, String.valueOf(session.getMemberId().value()), ttl);
    }

    @Override
    public Optional<AuthSession> findByMemberId(MemberId memberId) {
        String memberKey = memberKey(memberId);
        Map<Object, Object> fields = redisTemplate.opsForHash().entries(memberKey);
        if (fields.isEmpty()) return Optional.empty();
        return Optional.of(toAuthSession(fields));
    }

    @Override
    public Optional<AuthSession> findByRefreshToken(String refreshToken) {
        String tokenIndexKey = tokenIndexKey(refreshToken);
        String memberIdStr = redisTemplate.opsForValue().get(tokenIndexKey);
        if (memberIdStr == null) return Optional.empty();

        MemberId memberId = new MemberId(Long.valueOf(memberIdStr));
        return findByMemberId(memberId);
    }

    @Override
    public void delete(MemberId memberId) {
        String memberKey = memberKey(memberId);
        Map<Object, Object> fields = redisTemplate.opsForHash().entries(memberKey);
        if (!fields.isEmpty()) {
            String refreshToken = (String) fields.get("refreshToken");
            if (refreshToken != null) {
                redisTemplate.delete(tokenIndexKey(refreshToken));
            }
        }
        redisTemplate.delete(memberKey);
    }

    private AuthSession toAuthSession(Map<Object, Object> fields) {
        String sessionIdStr  = (String) fields.get("sessionId");
        String memberIdStr   = (String) fields.get("memberId");
        String refreshToken  = (String) fields.get("refreshToken");
        String expiresAtStr  = (String) fields.get("expiresAt");
        String revokedStr    = (String) fields.get("revoked");

        if (sessionIdStr == null || memberIdStr == null || refreshToken == null || expiresAtStr == null) {
            throw new IllegalStateException("Redis 세션 데이터가 손상되었습니다");
        }

        AuthSessionId sessionId = new AuthSessionId(sessionIdStr);
        MemberId memberId = new MemberId(Long.valueOf(memberIdStr));
        Instant expiresAt = Instant.ofEpochMilli(Long.parseLong(expiresAtStr));
        boolean revoked = Boolean.parseBoolean(revokedStr);

        return AuthSession.restore(sessionId, memberId, refreshToken, expiresAt, revoked);
    }

    private String memberKey(MemberId memberId) {
        return SESSION_BY_MEMBER_PREFIX + memberId.value();
    }

    private String tokenIndexKey(String token) {
        return TOKEN_INDEX_PREFIX + token;
    }
}
