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
    private final int inquiryPerMinute;
    private final int inquiryPerDay;
    private final int windowSeconds;

    public RateLimiterService(
            RedisTemplate<String, String> redisTemplate,
            @Value("${ovlo.rate-limit.login-limit:10}") int loginLimit,
            @Value("${ovlo.rate-limit.refresh-limit:30}") int refreshLimit,
            @Value("${ovlo.rate-limit.signup-limit:5}") int signupLimit,
            @Value("${ovlo.rate-limit.search-limit:100}") int searchLimit,
            @Value("${ovlo.rate-limit.inquiry-per-minute:5}") int inquiryPerMinute,
            @Value("${ovlo.rate-limit.inquiry-per-day:50}") int inquiryPerDay,
            @Value("${ovlo.rate-limit.window-seconds:600}") int windowSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.loginLimit = loginLimit;
        this.refreshLimit = refreshLimit;
        this.signupLimit = signupLimit;
        this.searchLimit = searchLimit;
        this.inquiryPerMinute = inquiryPerMinute;
        this.inquiryPerDay = inquiryPerDay;
        this.windowSeconds = windowSeconds;
    }

    public void checkLoginRate(String clientIp, String email) {
        check("rl:login:ip:" + clientIp, loginLimit);
        check("rl:login:acc:" + email, loginLimit * 3);
    }

    /**
     * 소셜 로그인(Google·Apple) — IP 기준만. 요청 시점엔 계정을 모르므로 checkLoginRate 의 계정 키에
     * 제공자 이름을 넣으면 모든 사용자가 한 버킷을 나눠 써, 아무나 그 버킷을 비워 전원을 막을 수 있다.
     */
    public void checkSocialLoginRate(String clientIp) {
        check("rl:login:ip:" + clientIp, loginLimit);
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

    /** 국제처 질문(LLM 호출) — 회원 기준 분·일 두 창. Office 단위 일일 상한은 유스케이스가 DB 로 센다. */
    public void checkOfficeInquiryRate(Long memberId) {
        check("rl:inquiry:min:member:" + memberId, inquiryPerMinute, 60);
        check("rl:inquiry:day:member:" + memberId, inquiryPerDay, 24 * 60 * 60);
    }

    /**
     * 회원 탈퇴 — 회원 기준. Apple 회원은 요청마다 Apple 토큰 교환·폐기를 외부 호출하므로,
     * 탈취된 액세스 토큰이나 재시도 루프가 Apple 쪽 호출을 무한히 일으키지 못하게 한다.
     * 정상 사용자는 재인증 실패 후 몇 번 다시 시도하는 정도라 10분에 5회면 충분하다.
     */
    public void checkWithdrawRate(Long memberId) { check("rl:withdraw:member:" + memberId, 5, 600); }

    /** 신고는 남용되면 관리자 대기열을 덮는다. 정상 사용은 10분에 몇 건이 넘지 않는다. */
    public void checkReportRate(Long member) { check("rl:report:" + member, 20, 600); }

    public void checkMarketplaceWriteRate(Long member) { check("rl:market:write:" + member, 30, 600); }
    public void checkMarketplaceUploadRate(Long member) { check("rl:market:photo:" + member, 80, 3600); }
    public void checkMarketplaceTranslationRate(Long member) {
        check("rl:market:translate:min:" + member, 10, 60);
        check("rl:market:translate:day:" + member, 100, 86400);
    }

    private void check(String key, int limit) {
        check(key, limit, windowSeconds);
    }

    private void check(String key, int limit, int ttlSeconds) {
        Long count = redisTemplate.execute(INCREMENT_SCRIPT, List.of(key), String.valueOf(ttlSeconds));
        if (count == null) throw new IllegalStateException("Rate limiter unavailable");
        if (count > limit) {
            throw new TooManyRequestsException("요청이 너무 많습니다. 잠시 후 다시 시도해주세요");
        }
    }
}
