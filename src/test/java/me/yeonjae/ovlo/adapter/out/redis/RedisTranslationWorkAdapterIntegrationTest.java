package me.yeonjae.ovlo.adapter.out.redis;

import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.*;
import java.time.Duration;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@Tag("integration")
@Testcontainers
class RedisTranslationWorkAdapterIntegrationTest {
    @Container static final GenericContainer<?> REDIS=new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    static LettuceConnectionFactory connection;
    static StringRedisTemplate redis;
    static RedisTranslationWorkAdapter sut;
    @BeforeAll static void connect() {
        connection=new LettuceConnectionFactory(REDIS.getHost(),REDIS.getMappedPort(6379));connection.afterPropertiesSet();connection.start();
        redis=new StringRedisTemplate(connection);sut=new RedisTranslationWorkAdapter(redis);
    }
    @AfterAll static void closeConnection() { if(connection!=null)connection.destroy(); }

    @Test void concurrentClaimsHaveExactlyOneOwnerAndReleaseAllowsRetry() throws Exception {
        String key="test:"+UUID.randomUUID();
        try(var pool=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks=java.util.stream.IntStream.range(0,16).mapToObj(i->pool.submit(()->sut.tryAcquire(key))).toList();
            var leases=new java.util.ArrayList<me.yeonjae.ovlo.application.port.out.office.TranslationWorkPort.Lease>();
            for(var task:tasks)task.get().ifPresent(leases::add);
            assertThat(leases).hasSize(1);assertThat(leases.getFirst().isHeld()).isTrue();
            assertThat(redis.getExpire("office:translation:"+key)).isBetween(1L,180L);
            leases.getFirst().close();assertThat(leases.getFirst().isHeld()).isFalse();
            try(var replacement=sut.tryAcquire(key).orElseThrow()) { assertThat(replacement.isHeld()).isTrue(); }
        }
    }
    @Test void expiredOwnerCannotDeleteReplacementLease() {
        String key="test:"+UUID.randomUUID();
        var old=sut.tryAcquire(key).orElseThrow();
        redis.expire("office:translation:"+key,Duration.ZERO);
        try(var replacement=sut.tryAcquire(key).orElseThrow()) {
            assertThat(old.isHeld()).isFalse();old.close();old.close();
            assertThat(replacement.isHeld()).isTrue();assertThat(sut.tryAcquire(key)).isEmpty();
        }
    }
}
