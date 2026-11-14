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
    private static final RedisScript<Long> RELEASE = RedisScript.of("""
        if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end
        return 0
        """, Long.class);
    private final RedisTemplate<String,String> redis;
    public RedisTranslationWorkAdapter(RedisTemplate<String,String> redis) { this.redis=redis; }
    @Override public Optional<Lease> tryAcquire(String workKey) {
        String key="office:translation:"+workKey;
        String owner=UUID.randomUUID().toString();
        if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key,owner,Duration.ofSeconds(180)))) return Optional.empty();
        return Optional.of(new Lease() {
            private boolean closed;
            public boolean isHeld() { return !closed && owner.equals(redis.opsForValue().get(key)); }
            public void close() {
                if (!closed) { closed=true; redis.execute(RELEASE,List.of(key),owner); }
            }
        });
    }
}
