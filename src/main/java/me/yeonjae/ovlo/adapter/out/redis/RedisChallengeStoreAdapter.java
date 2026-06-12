package me.yeonjae.ovlo.adapter.out.redis;

import me.yeonjae.ovlo.application.port.out.verification.ChallengeStorePort;
import me.yeonjae.ovlo.domain.verification.model.EmailVerificationChallenge;
import me.yeonjae.ovlo.domain.verification.model.VerificationCode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * 이메일 인증 챌린지 Redis 저장소.
 * 키: verification:challenge:member:{memberId} (Hash), TTL = 코드 만료시간.
 * 멤버당 1개만 유지(새 발송 시 덮어씀).
 */
@Component
public class RedisChallengeStoreAdapter implements ChallengeStorePort {

    private static final String PREFIX = "verification:challenge:member:";

    private final RedisTemplate<String, String> redisTemplate;

    public RedisChallengeStoreAdapter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(EmailVerificationChallenge challenge, Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            return;
        }
        String key = key(challenge.getMemberId());
        redisTemplate.opsForHash().putAll(key, Map.of(
                "universityId", String.valueOf(challenge.getUniversityId()),
                "targetEmail", challenge.getTargetEmail(),
                "code", challenge.getCode().value(),
                "expiresAt", String.valueOf(challenge.getExpiresAt().toEpochMilli()),
                "maxAttempts", String.valueOf(challenge.getMaxAttempts()),
                "attemptCount", String.valueOf(challenge.getAttemptCount())
        ));
        redisTemplate.expire(key, ttl);
    }

    @Override
    public Optional<EmailVerificationChallenge> findByMemberId(Long memberId) {
        String key = key(memberId);
        Map<Object, Object> h = redisTemplate.opsForHash().entries(key);
        if (h.isEmpty()) {
            return Optional.empty();
        }
        EmailVerificationChallenge challenge = EmailVerificationChallenge.restore(
                memberId,
                Long.valueOf((String) h.get("universityId")),
                (String) h.get("targetEmail"),
                new VerificationCode((String) h.get("code")),
                Instant.ofEpochMilli(Long.parseLong((String) h.get("expiresAt"))),
                Integer.parseInt((String) h.get("maxAttempts")),
                Integer.parseInt((String) h.get("attemptCount"))
        );
        return Optional.of(challenge);
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        redisTemplate.delete(key(memberId));
    }

    private String key(Long memberId) {
        return PREFIX + memberId;
    }
}
