package me.yeonjae.ovlo.adapter.out.redis;

import me.yeonjae.ovlo.application.port.out.auth.TokenStorePort;
import me.yeonjae.ovlo.domain.auth.model.AuthSession;
import me.yeonjae.ovlo.domain.auth.model.AuthSessionId;
import me.yeonjae.ovlo.domain.auth.model.RefreshRotationOutcome;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.shared.security.TokenHashUtil;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.script.RedisScript;
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
 *   auth:session:{sessionId}           — 세션 Hash (기기별 독립). refreshToken·prevRefreshToken 필드는
 *                                        SHA-256 해시, rotatedAt 은 마지막 교체 시각(ms)
 *   auth:member:sessions:{memberId}    — 해당 멤버의 sessionId Set
 *   auth:token:{sha256(refreshToken)}  — refreshToken 해시 → sessionId 역인덱스
 *                                        (직전 토큰 것은 재사용 감지 창만큼만 유지)
 *
 * 보안: refresh token은 고엔트로피이므로 SHA-256 단방향 해시만 저장한다. Redis가
 * 유출되어도 원문 토큰을 복원할 수 없어 세션 탈취를 막는다 (GLOBAL-PIT-001).
 */
@Component
public class RedisTokenAdapter implements TokenStorePort {

    private static final String SESSION_PREFIX = "auth:session:";
    private static final String MEMBER_SESSIONS_PREFIX = "auth:member:sessions:";
    private static final String TOKEN_INDEX_PREFIX = "auth:token:";

    /**
     * 교체 직후 직전 토큰이 다시 오면 동시 재발급 경합(다중 탭·중복 호출)에서 진 요청으로 본다.
     * 이긴 요청의 Set-Cookie가 이미 쿠키 저장소에 들어갔을 시간이라, 클라이언트는 409를 받고 한 번
     * 재시도하면 새 토큰으로 성공한다.
     */
    static final Duration CONCURRENT_GRACE = Duration.ofSeconds(10);

    /**
     * 교체된 직전 토큰의 역인덱스를 남겨두는 기간. 이 기간 안에 직전 토큰이 유예 시간을 넘겨 다시
     * 오면 REUSED로 판정해 보안 로그를 남긴다. 그보다 오래된 토큰은 INVALID로만 보인다.
     */
    static final Duration REUSE_DETECTION_WINDOW = Duration.ofDays(1);

    /**
     * 제출 토큰 기준 compare-and-rotate. 비교·판정·쓰기를 Lua 한 번으로 실행해 원자적이다.
     * KEYS: 1 세션 Hash, 2 제출 토큰 역인덱스, 3 새 토큰 역인덱스, 4 멤버 세션 Set
     * ARGV: 1 제출 토큰 해시, 2 새 토큰 해시, 3 now(ms), 4 새 만료(ms), 5 sessionId,
     *       6 세션 TTL(s), 7 재사용 감지 창(s), 8 멤버 Set TTL(s), 9 유예 시간(ms)
     * 세션이 없으면 아무것도 쓰지 않으므로 로그아웃과 경합해도 세션이 되살아나지 않는다.
     */
    private static final RedisScript<String> ROTATE_SCRIPT = RedisScript.of("""
            local s = redis.call('HMGET', KEYS[1], 'refreshToken', 'prevRefreshToken', 'rotatedAt', 'revoked', 'expiresAt')
            local current, previous, rotatedAt, revoked, expiresAt = s[1], s[2], s[3], s[4], s[5]
            local presented, now = ARGV[1], tonumber(ARGV[3])
            if not current or not expiresAt or revoked == 'true' or tonumber(expiresAt) <= now then
              return 'INVALID'
            end
            if current == presented then
              redis.call('HSET', KEYS[1], 'refreshToken', ARGV[2], 'prevRefreshToken', presented,
                         'rotatedAt', ARGV[3], 'expiresAt', ARGV[4])
              redis.call('EXPIRE', KEYS[1], ARGV[6])
              redis.call('SET', KEYS[3], ARGV[5], 'EX', ARGV[6])
              redis.call('EXPIRE', KEYS[2], ARGV[7])
              redis.call('SADD', KEYS[4], ARGV[5])
              redis.call('EXPIRE', KEYS[4], ARGV[8])
              return 'ROTATED'
            end
            if previous and previous == presented then
              if rotatedAt and now - tonumber(rotatedAt) <= tonumber(ARGV[9]) then
                return 'CONCURRENT'
              end
              return 'REUSED'
            end
            return 'INVALID'
            """, String.class);

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

        // 새 세션(무작위 sessionId)이라 읽고-판단할 이전 상태가 없다 — 쓰기만 MULTI/EXEC로 묶는다.
        // 기존 세션의 토큰 교체는 제출 토큰 비교가 필요하므로 rotate()가 맡는다.
        redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            @SuppressWarnings("unchecked")
            public <K, V> List<Object> execute(RedisOperations<K, V> ops) {
                RedisOperations<String, String> operations = (RedisOperations<String, String>) ops;
                operations.multi();
                operations.opsForHash().putAll(sessionKey, fields);
                operations.expire(sessionKey, ttl);
                operations.opsForSet().add(memberSessionsKey, session.getId().value());
                operations.expire(memberSessionsKey, ttl.plusDays(1));
                operations.opsForValue().set(tokenIndexKey, session.getId().value(), ttl);
                return operations.exec();
            }
        });
    }

    @Override
    public RefreshRotationOutcome rotate(AuthSession rotated, String presentedToken) {
        Instant now = Instant.now();
        Duration ttl = Duration.between(now, rotated.getExpiresAt());
        if (ttl.isNegative() || ttl.isZero()) return RefreshRotationOutcome.INVALID;

        String presentedHash = TokenHashUtil.sha256(presentedToken);
        String newHash = TokenHashUtil.sha256(rotated.getRefreshToken());
        String sessionId = rotated.getId().value();

        List<String> keys = List.of(
                sessionKey(rotated.getId()),
                tokenIndexKey(presentedHash),
                tokenIndexKey(newHash),
                memberSessionsKey(rotated.getMemberId()));
        String result = redisTemplate.execute(ROTATE_SCRIPT, keys,
                presentedHash,
                newHash,
                String.valueOf(now.toEpochMilli()),
                String.valueOf(rotated.getExpiresAt().toEpochMilli()),
                sessionId,
                String.valueOf(ttl.toSeconds()),
                String.valueOf(REUSE_DETECTION_WINDOW.toSeconds()),
                String.valueOf(ttl.plusDays(1).toSeconds()),
                String.valueOf(CONCURRENT_GRACE.toMillis()));

        if (result == null) {
            throw new IllegalStateException("refresh 토큰 교체 스크립트가 결과를 반환하지 않았습니다: " + sessionId);
        }
        return RefreshRotationOutcome.valueOf(result);
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
