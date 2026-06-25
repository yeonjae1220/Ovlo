package me.yeonjae.ovlo.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Admin formLogin(/admin/login) 무차별 대입 방지용 실패 카운터 + lockout 저장소.
 *
 * <p>IP와 admin username(이메일) 두 차원으로 로그인 실패를 센다. 한 차원에서
 * 임계값({@code ipMaxAttempts} / {@code accountMaxAttempts})회 실패가
 * {@code failWindowSeconds} 안에 누적되면 그 차원에 {@code lockoutSeconds} 동안
 * 락을 건다. IP 임계값(기본 5/5분 → 15분)은 lab-dashboard 콘솔과 동일하다.
 *
 * <p>계정 차원은 IP보다 <b>높은 임계값</b>(기본 20)을 쓴다. admin 계정이 단일
 * 고정값이라, 공격자가 임의 IP에서 소수의 오답만으로 정상 관리자를 잠그는
 * 가용성 DoS를 줄이기 위함이다. 분산 IP 무차별 대입(여러 IP에서 같은 계정 공략)은
 * 여전히 계정 임계값에서 차단된다.
 *
 * <p>스프링 API 컨트롤러에만 수동 호출되는 {@code RateLimiterService}와 동일한
 * Redis 원자적 Lua 패턴을 사용해, 멀티 레플리카/크래시 상황에서도 카운트가
 * 일관되게 유지된다. IP는 신뢰 프록시 헤더를 검증하는 {@link ClientIpResolver}로
 * 해석한 값을 받는다(헤더 위조로 IP 차원을 우회하지 못하게 함).
 */
@Component
public class AdminLoginAttemptService {

    /**
     * 실패 카운터를 INCR 하고, 새 키면 EXPIRE(window)로 슬라이딩 윈도우를 설정한다.
     * 누적 실패가 maxAttempts에 도달하면 lockout 키를 SET EX(lockout)로 걸고
     * 실패 카운터는 DEL 해 다음 윈도우를 깨끗하게 시작한다. 모두 단일 스크립트라
     * 크래시가 나도 INCR/EXPIRE/SET 사이가 찢어지지 않는다.
     */
    private static final RedisScript<Long> RECORD_FAILURE_SCRIPT = RedisScript.of(
            "local c = redis.call('INCR', KEYS[1])\n" +
            "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end\n" +
            "if c >= tonumber(ARGV[2]) then\n" +
            "  redis.call('SET', KEYS[2], '1', 'EX', ARGV[3])\n" +
            "  redis.call('DEL', KEYS[1])\n" +
            "end\n" +
            "return c",
            Long.class);

    private static final String FAIL_IP_PREFIX = "admin:login:fail:ip:";
    private static final String LOCK_IP_PREFIX = "admin:login:lock:ip:";
    private static final String FAIL_ACC_PREFIX = "admin:login:fail:acc:";
    private static final String LOCK_ACC_PREFIX = "admin:login:lock:acc:";

    private final RedisTemplate<String, String> redisTemplate;
    private final int ipMaxAttempts;
    private final int accountMaxAttempts;
    private final int failWindowSeconds;
    private final int lockoutSeconds;

    public AdminLoginAttemptService(
            RedisTemplate<String, String> redisTemplate,
            @Value("${ovlo.admin-login.ip-max-attempts:5}") int ipMaxAttempts,
            @Value("${ovlo.admin-login.account-max-attempts:20}") int accountMaxAttempts,
            @Value("${ovlo.admin-login.window-seconds:300}") int failWindowSeconds,
            @Value("${ovlo.admin-login.lockout-seconds:900}") int lockoutSeconds) {
        this.redisTemplate = redisTemplate;
        this.ipMaxAttempts = ipMaxAttempts;
        this.accountMaxAttempts = accountMaxAttempts;
        this.failWindowSeconds = failWindowSeconds;
        this.lockoutSeconds = lockoutSeconds;
    }

    /** 현재 IP 또는 계정에 락이 걸려 있는지와, 걸려 있다면 남은 시간(초)을 반환. */
    public LockStatus checkLock(String clientIp, String username) {
        long ipRemaining = remainingLock(LOCK_IP_PREFIX + clientIp);
        String account = normalize(username);
        long accRemaining = account == null ? -2L : remainingLock(LOCK_ACC_PREFIX + account);
        long remaining = Math.max(ipRemaining, accRemaining);
        return remaining > 0 ? new LockStatus(true, remaining) : LockStatus.unlocked();
    }

    /** 로그인 실패 1회 기록 — IP 차원, (username이 있으면) 계정 차원 각각 카운트. */
    public void recordFailure(String clientIp, String username) {
        record(FAIL_IP_PREFIX + clientIp, LOCK_IP_PREFIX + clientIp, ipMaxAttempts);
        String account = normalize(username);
        if (account != null) {
            record(FAIL_ACC_PREFIX + account, LOCK_ACC_PREFIX + account, accountMaxAttempts);
        }
    }

    /** 로그인 성공 — 해당 IP·계정의 실패 카운터와 락을 모두 비운다. */
    public void recordSuccess(String clientIp, String username) {
        String account = normalize(username);
        if (account != null) {
            redisTemplate.delete(List.of(
                    FAIL_IP_PREFIX + clientIp, LOCK_IP_PREFIX + clientIp,
                    FAIL_ACC_PREFIX + account, LOCK_ACC_PREFIX + account));
        } else {
            redisTemplate.delete(List.of(FAIL_IP_PREFIX + clientIp, LOCK_IP_PREFIX + clientIp));
        }
    }

    private void record(String failKey, String lockKey, int maxAttempts) {
        redisTemplate.execute(RECORD_FAILURE_SCRIPT, List.of(failKey, lockKey),
                String.valueOf(failWindowSeconds),
                String.valueOf(maxAttempts),
                String.valueOf(lockoutSeconds));
    }

    private long remainingLock(String lockKey) {
        Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        return ttl == null ? -2L : ttl;
    }

    private static String normalize(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    /** 락 상태와 재시도까지 남은 시간(초). retryAfterSeconds는 locked일 때만 의미를 가진다. */
    public record LockStatus(boolean locked, long retryAfterSeconds) {
        public static LockStatus unlocked() {
            return new LockStatus(false, 0L);
        }
    }
}
