package me.yeonjae.ovlo.adapter.out.redis;

import me.yeonjae.ovlo.application.port.out.auth.TokenStorePort;
import me.yeonjae.ovlo.domain.auth.model.AuthSession;
import me.yeonjae.ovlo.domain.auth.model.AuthSessionId;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.data.redis.core.RedisTemplate;
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

        redisTemplate.opsForHash().putAll(memberKey, fields);
        redisTemplate.expire(memberKey, ttl);

        // token → memberId 역인덱스 (빠른 조회용)
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
        AuthSessionId sessionId = new AuthSessionId((String) fields.get("sessionId"));
        MemberId memberId = new MemberId(Long.valueOf((String) fields.get("memberId")));
        String refreshToken = (String) fields.get("refreshToken");
        Instant expiresAt = Instant.ofEpochMilli(Long.parseLong((String) fields.get("expiresAt")));
        boolean revoked = Boolean.parseBoolean((String) fields.get("revoked"));

        return AuthSession.restore(sessionId, memberId, refreshToken, expiresAt, revoked);
    }

    private String memberKey(MemberId memberId) {
        return SESSION_BY_MEMBER_PREFIX + memberId.value();
    }

    private String tokenIndexKey(String token) {
        return TOKEN_INDEX_PREFIX + token;
    }
}
