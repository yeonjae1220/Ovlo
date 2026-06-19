package me.yeonjae.ovlo.shared.security;

import me.yeonjae.ovlo.shared.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis Lua 스크립트 기반 슬라이딩 카운터 Rate Limiter.
 * INCR + EXPIRE 를 단일 Lua 스크립트로 원자적 실행 — 크래시 시 TTL 누락 방지.
 */
@Component
public class RateLimiterService {

    /**
     * INCR 후 키가 새로 생성(count == 1)될 때만 EXPIRE 설정.
     * 두 명령이 원자적으로 실행되므로 크래시가 발생해도 TTL 누락 없음.
     */
    private static final RedisScript<Long> INCREMENT_SCRIPT = RedisScript.of(
            "local c = redis.call('INCR', KEYS[1])\n" +
            "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end\n" +
            "return c",
            Long.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final int loginLimit;
    private final int refreshLimit;
    private final int signupLimit;
    private final int searchLimit;
    private final int windowSeconds;

    public RateLimiterService(
            RedisTemplate<String, String> redisTemplate,
            @Value("${ovlo.rate-limit.login-limit:10}") int loginLimit,
            @Value("${ovlo.rate-limit.refresh-limit:30}") int refreshLimit,
            @Value("${ovlo.rate-limit.signup-limit:5}") int signupLimit,
            @Value("${ovlo.rate-limit.search-limit:100}") int searchLimit,
            @Value("${ovlo.rate-limit.window-seconds:600}") int windowSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.loginLimit = loginLimit;
        this.refreshLimit = refreshLimit;
        this.signupLimit = signupLimit;
        this.searchLimit = searchLimit;
        this.windowSeconds = windowSeconds;
    }

    public void checkLoginRate(String clientIp, String email) {
        check("rl:login:ip:" + clientIp, loginLimit);
        check("rl:login:acc:" + email, loginLimit * 3);
    }

    public void checkRefreshRate(String clientIp) {
        check("rl:refresh:" + clientIp, refreshLimit);
    }

    public void checkSignupRate(String clientIp) {
        check("rl:signup:ip:" + clientIp, signupLimit);
    }

    /** 학교 이메일 인증 코드 발송 — 인증된 멤버 기준(발송 남용/스팸 차단). */
    public void checkEmailVerificationRate(Long memberId) {
        check("rl:verify-email:member:" + memberId, signupLimit);
    }

    /** 공개 대학 카탈로그 검색 — 익명 엔드포인트라 IP 기준(ILIKE 스캔 남용/DoS 완화). */
    public void checkSearchRate(String clientIp) {
        check("rl:search:ip:" + clientIp, searchLimit);
    }

    private void check(String key, int limit) {
        Long count = redisTemplate.execute(INCREMENT_SCRIPT, List.of(key), String.valueOf(windowSeconds));
        if (count != null && count > limit) {
            throw new TooManyRequestsException("요청이 너무 많습니다. 잠시 후 다시 시도해주세요");
        }
    }
}
