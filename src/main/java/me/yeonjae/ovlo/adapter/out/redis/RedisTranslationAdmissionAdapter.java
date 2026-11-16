package me.yeonjae.ovlo.adapter.out.redis;

import me.yeonjae.ovlo.application.port.out.office.TranslationAdmissionPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

/** Atomic admission across app instances. Redis failure never permits a paid call. */
@Component
public class RedisTranslationAdmissionAdapter implements TranslationAdmissionPort {
    private static final RedisScript<Long> ACQUIRE = RedisScript.of("""
        local time = redis.call('TIME')
        local now = tonumber(time[1])
        local day = math.floor(now / 86400)
        -- A hash per scope uses Redis time, so midnight and app clock skew cannot bypass limits.
        for i=1,3 do
            redis.call('ZREMRANGEBYSCORE', KEYS[i+3], '-inf', now)
            if redis.call('ZCARD', KEYS[i+3]) >= tonumber(ARGV[i+6]) then return -30 end
            local count, budget = 0, 0
            if tonumber(redis.call('HGET', KEYS[i], 'day') or '-1') == day then
                count = tonumber(redis.call('HGET', KEYS[i], 'count') or '0')
                budget = tonumber(redis.call('HGET', KEYS[i], 'budget') or '0')
            end
            if count >= tonumber(ARGV[i]) or budget + tonumber(ARGV[10]) > tonumber(ARGV[i+3]) then return -(86400 - now % 86400 + 1) end
        end
        for i=1,3 do
            if tonumber(redis.call('HGET', KEYS[i], 'day') or '-1') ~= day then
                redis.call('HSET', KEYS[i], 'day', day, 'count', 0, 'budget', 0)
            end
            redis.call('HINCRBY', KEYS[i], 'count', 1)
            redis.call('HINCRBY', KEYS[i], 'budget', ARGV[10])
            redis.call('EXPIRE', KEYS[i], 172800)
            redis.call('ZADD', KEYS[i+3], now+tonumber(ARGV[12]), ARGV[11])
            redis.call('EXPIRE', KEYS[i+3], 600)
        end
        return 1
        """, Long.class);
    private static final RedisScript<Long> RELEASE = RedisScript.of("""
        for i=1,3 do redis.call('ZREM', KEYS[i], ARGV[1]) end
        return 1
        """, Long.class);
    private final RedisTemplate<String,String> redis;
    private final List<String> limits;
    public RedisTranslationAdmissionAdapter(RedisTemplate<String,String> redis,
            @Value("${ovlo.office.translation-member-daily-limit:100}") int memberDaily,
            @Value("${ovlo.office.translation-office-daily-limit:2000}") int officeDaily,
            @Value("${ovlo.office.translation-global-daily-limit:10000}") int globalDaily,
            @Value("${ovlo.office.translation-member-token-budget:10000000}") long memberBudget,
            @Value("${ovlo.office.translation-office-token-budget:50000000}") long officeBudget,
            @Value("${ovlo.office.translation-global-token-budget:250000000}") long globalBudget,
            @Value("${ovlo.office.translation-office-concurrent-limit:5}") int officeConcurrent,
            @Value("${ovlo.office.translation-global-concurrent-limit:20}") int globalConcurrent) {
        if (memberDaily<1 || officeDaily<1 || globalDaily<1 || memberBudget<RESERVATION || officeBudget<RESERVATION
                || globalBudget<RESERVATION || officeConcurrent<1 || globalConcurrent<1) throw new IllegalArgumentException("Invalid translation limits");
        this.redis=redis;
        limits=List.of(""+memberDaily,""+officeDaily,""+globalDaily,""+memberBudget,""+officeBudget,""+globalBudget,
                "1",""+officeConcurrent,""+globalConcurrent,""+RESERVATION);
    }
    @Override public Lease acquire(Long officeId, Long memberId) { return acquire(officeId, memberId, 1); }
    @Override public Lease acquire(Long officeId, Long memberId, int maxProviderCalls) {
        if (maxProviderCalls < 1 || maxProviderCalls > 2) throw new IllegalArgumentException("Invalid provider call reservation");
        if (officeId==null || memberId==null) throw new IllegalArgumentException("Translation owner required");
        String prefix="ai:{translation}:";
        List<String> scopes=List.of(prefix+"member:"+memberId,prefix+"office:"+officeId,prefix+"global");
        List<String> active=scopes.stream().map(s->s+":active").toList();
        var keys=new java.util.ArrayList<>(scopes.stream().map(s->s+":daily").toList()); keys.addAll(active);
        String owner=UUID.randomUUID().toString();
        var args=new java.util.ArrayList<>(limits);
        args.set(9, Long.toString(RESERVATION * maxProviderCalls)); args.add(owner); args.add(maxProviderCalls == 2 ? "300" : "180");
        Long outcome=redis.execute(ACQUIRE,keys,args.toArray());
        if (outcome == null) throw new IllegalStateException("Translation admission unavailable");
        if (outcome != 1) throw new Unavailable(Math.max(30, -outcome));
        return new Lease() {
            private boolean closed;
            public void close() { if (!closed) { closed=true; redis.execute(RELEASE,active,owner); } }
        };
    }
}
