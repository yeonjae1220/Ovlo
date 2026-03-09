package me.yeonjae.ovlo.shared.security;

import me.yeonjae.ovlo.shared.exception.TooManyRequestsException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis INCR 기반 슬라이딩 카운터 Rate Limiter.
 * 첫 요청 시 키 생성 + TTL 설정, 이후 카운트만 증가.
 */
@Component
public class RateLimiterService {

    private static final int LOGIN_LIMIT = 10;    // 10분 내 최대 10회
    private static final int REFRESH_LIMIT = 30;  // 10분 내 최대 30회
    private static final int WINDOW_SECONDS = 600;

    private final RedisTemplate<String, String> redisTemplate;

    public RateLimiterService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void checkLoginRate(String clientIp) {
        check("rl:login:" + clientIp, LOGIN_LIMIT);
    }

    public void checkRefreshRate(String clientIp) {
        check("rl:refresh:" + clientIp, REFRESH_LIMIT);
    }

    private void check(String key, int limit) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            // 첫 요청일 때만 TTL 설정 (원자성 보장 불필요 — 만료 누락 시 다음 요청에서 재설정됨)
            redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
        }
        if (count != null && count > limit) {
            throw new TooManyRequestsException("요청이 너무 많습니다. 잠시 후 다시 시도해주세요");
        }
    }
}
