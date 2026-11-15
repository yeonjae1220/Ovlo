package me.yeonjae.ovlo.adapter.out.redis;

import me.yeonjae.ovlo.application.port.out.office.TranslationWorkPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Lease exceeds the provider's 90-second timeout. Expired owners cannot release a replacement owner's lease. */
@Component
public class RedisTranslationWorkAdapter implements TranslationWorkPort {
    // Retry state is shared by all app instances and callers, independently of the 180s work lease.
    // Reserve before the paid call: a crashed worker still consumes an attempt. Redis TIME avoids clock skew.
    private static final RedisScript<Long> BEGIN_ATTEMPT = RedisScript.of("""
        if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
        local time = redis.call('TIME')
        local now = time[1] * 1000 + math.floor(time[2] / 1000)
        local attempts = tonumber(redis.call('HGET', KEYS[2], 'attempts') or '0')
        local nextAt = tonumber(redis.call('HGET', KEYS[2], 'nextAt') or '0')
        if attempts >= 3 or nextAt > now then return 0 end
        redis.call('HSET', KEYS[2], 'attempts', attempts + 1, 'nextAt', now + 180000)
        if attempts == 0 then redis.call('EXPIRE', KEYS[2], 86400) end
        return 1
        """, Long.class);
    private static final RedisScript<Long> FAILED = RedisScript.of("""
        if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
        local attempts = tonumber(redis.call('HGET', KEYS[2], 'attempts') or '0')
        if attempts == 0 then return 0 end
        local time = redis.call('TIME')
        local now = time[1] * 1000 + math.floor(time[2] / 1000)
        local delay = attempts == 1 and 60000 or 300000
        redis.call('HSET', KEYS[2], 'nextAt', now + delay)
        return 1
        """, Long.class);
    private static final RedisScript<Long> SUCCEEDED = RedisScript.of("""
        if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[2]) end
        return 0
        """, Long.class);
    private static final RedisScript<Long> RELEASE = RedisScript.of("""
        if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end
        return 0
        """, Long.class);
    private final RedisTemplate<String,String> redis;
    public RedisTranslationWorkAdapter(RedisTemplate<String,String> redis) { this.redis=redis; }
    @Override public Optional<Lease> tryAcquire(String workKey) {
        String key="office:translation:"+workKey;
        String retryKey="office:translation:retry:"+workKey;
        String owner=UUID.randomUUID().toString();
        if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key,owner,Duration.ofSeconds(180)))) return Optional.empty();
        return Optional.of(new Lease() {
            private boolean closed;
            private boolean attempted;
            public boolean isHeld() { return !closed && owner.equals(redis.opsForValue().get(key)); }
            public boolean tryBeginAttempt() {
                if (closed || attempted) return false;
                attempted = Long.valueOf(1).equals(redis.execute(BEGIN_ATTEMPT,List.of(key,retryKey),owner));
                return attempted;
            }
            public void translationFailed() {
                if (!closed && attempted) redis.execute(FAILED,List.of(key,retryKey),owner);
            }
            public void translationSucceeded() {
                if (!closed && attempted) redis.execute(SUCCEEDED,List.of(key,retryKey),owner);
            }
            public void close() {
                if (!closed) { closed=true; redis.execute(RELEASE,List.of(key),owner); }
            }
        });
    }
}
