package me.yeonjae.ovlo.adapter.out.redis;

import me.yeonjae.ovlo.application.port.out.office.InquiryAdmissionPort;
import me.yeonjae.ovlo.domain.office.exception.InquiryException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/** Single Redis script reserves counts, weighted tokens and concurrency. Redis errors never allow a call. */
@Component
public class RedisInquiryAdmissionAdapter implements InquiryAdmissionPort {
    private static final RedisScript<Long> ACQUIRE = RedisScript.of("""
        local now = tonumber(ARGV[1])
        redis.call('ZREMRANGEBYSCORE', KEYS[5], '-inf', now)
        redis.call('ZREMRANGEBYSCORE', KEYS[6], '-inf', now)
        if redis.call('EXISTS', KEYS[7]) == 1 then return 0 end
        if redis.call('ZCARD', KEYS[5]) >= tonumber(ARGV[7]) or redis.call('ZCARD', KEYS[6]) >= tonumber(ARGV[8]) then return 0 end
        if tonumber(redis.call('GET', KEYS[1]) or '0') >= tonumber(ARGV[3]) then return 0 end
        if tonumber(redis.call('GET', KEYS[2]) or '0') >= tonumber(ARGV[4]) then return 0 end
        if tonumber(redis.call('GET', KEYS[3]) or '0') + tonumber(ARGV[9]) > tonumber(ARGV[5]) then return 0 end
        if tonumber(redis.call('GET', KEYS[4]) or '0') + tonumber(ARGV[9]) > tonumber(ARGV[6]) then return 0 end
        for i=1,2 do redis.call('INCR', KEYS[i]); redis.call('EXPIRE', KEYS[i], 172800) end
        for i=3,4 do redis.call('INCRBY', KEYS[i], ARGV[9]); redis.call('EXPIRE', KEYS[i], 172800) end
        for i=5,6 do redis.call('ZADD', KEYS[i], now+600000, ARGV[2]); redis.call('EXPIRE', KEYS[i], 1200) end
        redis.call('SET', KEYS[7], ARGV[2], 'EX', 600)
        return 1
        """, Long.class);
    private static final RedisScript<Long> RELEASE = RedisScript.of("""
        if redis.call('GET', KEYS[7]) ~= ARGV[1] then return 0 end
        redis.call('DEL', KEYS[7])
        redis.call('ZREM', KEYS[5], ARGV[1]); redis.call('ZREM', KEYS[6], ARGV[1])
        for i=3,4 do
            if redis.call('EXISTS', KEYS[i]) == 1 then redis.call('INCRBY', KEYS[i], ARGV[2]) end
        end
        return 1
        """, Long.class);
    private final RedisTemplate<String, String> redis;
    private final Clock clock;
    private final int officeDaily, globalDaily, officeConcurrent, globalConcurrent;
    private final long officeBudget, globalBudget;
    public RedisInquiryAdmissionAdapter(RedisTemplate<String, String> redis, Clock clock,
        @Value("${ovlo.office.inquiry-office-daily-limit:500}") int officeDaily,
        @Value("${ovlo.office.ai-global-daily-limit:5000}") int globalDaily,
        @Value("${ovlo.office.ai-office-weighted-token-budget:25000000}") long officeBudget,
        @Value("${ovlo.office.ai-global-weighted-token-budget:100000000}") long globalBudget,
        @Value("${ovlo.office.ai-office-concurrent-limit:5}") int officeConcurrent,
        @Value("${ovlo.office.ai-global-concurrent-limit:20}") int globalConcurrent) {
        if (officeDaily < 1 || globalDaily < 1 || officeBudget < RESERVATION || globalBudget < RESERVATION
            || officeConcurrent < 1 || globalConcurrent < 1) throw new IllegalArgumentException("Invalid AI limits");
        this.redis=redis; this.clock=clock; this.officeDaily=officeDaily; this.globalDaily=globalDaily;
        this.officeBudget=officeBudget; this.globalBudget=globalBudget;
        this.officeConcurrent=officeConcurrent; this.globalConcurrent=globalConcurrent;
    }
    @Override public Lease acquire(Long officeId, Long memberId) {
        String day=clock.instant().atZone(ZoneOffset.UTC).toLocalDate().toString();
        // Common hash tag also keeps atomic operations on one Redis Cluster slot.
        String prefix="ai:{inquiry}:";
        List<String> keys=List.of(prefix+day+":office:"+officeId+":count", prefix+day+":global:count",
            prefix+day+":office:"+officeId+":budget", prefix+day+":global:budget",
            prefix+"office:"+officeId+":active", prefix+"global:active", prefix+"member:"+memberId+":active");
        String owner=UUID.randomUUID().toString();
        Long result=redis.execute(ACQUIRE, keys, String.valueOf(clock.millis()), owner,
            String.valueOf(officeDaily), String.valueOf(globalDaily), String.valueOf(officeBudget),
            String.valueOf(globalBudget), String.valueOf(officeConcurrent), String.valueOf(globalConcurrent), String.valueOf(RESERVATION));
        if (!Long.valueOf(1).equals(result)) throw new InquiryException("진행 중인 질문이 있거나 오늘의 이용 한도에 도달했습니다. 잠시 후 다시 시도해 주세요.", InquiryException.ErrorType.RATE_LIMITED);
        return new Lease() {
            private long used;
            private boolean complete, closed;
            public void usage(long input, long output) {
                if(input < 0 || output < 0) throw new IllegalArgumentException("Invalid token usage");
                used=Math.addExact(used, Math.addExact(input, Math.multiplyExact(output, OUTPUT_WEIGHT)));
            }
            public void complete() { complete=true; }
            public void close() {
                if(closed) return;
                closed=true;
                // Unknown provider usage (timeout/crash) retains the entire reservation.
                long delta=complete ? used-RESERVATION : Math.max(0, used-RESERVATION);
                redis.execute(RELEASE, keys, owner, String.valueOf(delta));
            }
        };
    }
}
