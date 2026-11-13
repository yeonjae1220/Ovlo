package me.yeonjae.ovlo.adapter.out.redis;

import me.yeonjae.ovlo.application.port.out.office.InquiryAdmissionPort;
import me.yeonjae.ovlo.domain.office.exception.InquiryException;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@Tag("integration") @Testcontainers
class RedisInquiryAdmissionAdapterIntegrationTest {
    @Container static GenericContainer<?> redisContainer=new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    static LettuceConnectionFactory connection;
    static StringRedisTemplate redis;
    static Clock clock=Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZoneOffset.UTC);
    @BeforeAll static void connect() {
        connection=new LettuceConnectionFactory(redisContainer.getHost(), redisContainer.getMappedPort(6379));
        connection.afterPropertiesSet(); connection.start(); redis=new StringRedisTemplate(connection);
    }
    @AfterAll static void disconnect() { connection.destroy(); }
    @BeforeEach void clear() { try(var c=connection.getConnection()) { c.serverCommands().flushAll(); } }
    RedisInquiryAdmissionAdapter adapter(int office, int global, long budget) {
        return new RedisInquiryAdmissionAdapter(redis,clock,office,global,budget,budget,40,40);
    }
    @Test void concurrentRequestsCannotOvershootLastOfficeSlot() throws Exception {
        var adapter=adapter(1,100,100*InquiryAdmissionPort.RESERVATION);
        var barrier=new CyclicBarrier(20);
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            var futures=new ArrayList<Future<Boolean>>();
            for(long member=1;member<=20;member++) {
                long id=member;
                futures.add(pool.submit(() -> { barrier.await();
                    try(var lease=adapter.acquire(1L,id)) { lease.complete(); return true; }
                    catch(InquiryException e) { return false; }
                }));
            }
            int successes=0; for(var f:futures) if(f.get()) successes++;
            assertThat(successes).isEqualTo(1);
        }
        assertThat(redis.opsForValue().get("ai:{inquiry}:2026-11-13:office:1:count")).isEqualTo("1");
    }
    @Test void memberCannotBypassConcurrencyByChangingSchool() {
        var adapter=adapter(100,100,100*InquiryAdmissionPort.RESERVATION);
        try(var first=adapter.acquire(1L,7L)) {
            assertThatThrownBy(() -> adapter.acquire(2L,7L)).isInstanceOf(InquiryException.class);
            first.complete();
        }
        try(var second=adapter.acquire(2L,7L)) { second.complete(); }
    }
    @Test void unknownUsageKeepsBudgetAndKnownUsageRefundsOnlyUnusedReservation() {
        var adapter=adapter(100,100,InquiryAdmissionPort.RESERVATION);
        try(var lease=adapter.acquire(1L,1L)) { lease.usage(10,2); lease.complete(); }
        assertThat(redis.opsForValue().get("ai:{inquiry}:2026-11-13:global:budget")).isEqualTo("20");
        assertThatThrownBy(() -> adapter.acquire(2L,2L)).isInstanceOf(InquiryException.class);
        clear();
        try(var lease=adapter.acquire(1L,1L)) { lease.usage(10,2); }
        assertThat(redis.opsForValue().get("ai:{inquiry}:2026-11-13:global:budget")).isEqualTo(String.valueOf(InquiryAdmissionPort.RESERVATION));
        assertThatThrownBy(() -> adapter.acquire(2L,2L)).isInstanceOf(InquiryException.class);
    }
    @Test void globalLimitAppliesAcrossSchoolsAndCloseIsIdempotent() {
        var adapter=adapter(100,1,100*InquiryAdmissionPort.RESERVATION);
        var lease=adapter.acquire(1L,1L); lease.usage(100,10); lease.complete(); lease.close(); lease.close();
        assertThat(redis.opsForValue().get("ai:{inquiry}:2026-11-13:global:budget")).isEqualTo("150");
        assertThatThrownBy(() -> adapter.acquire(2L,2L)).isInstanceOf(InquiryException.class);
    }
    @Test void expiredOwnerCannotReleaseNewOwnerOrRefundReservation() {
        var adapter=adapter(100,100,100*InquiryAdmissionPort.RESERVATION);
        var old=adapter.acquire(1L,1L);
        redis.delete("ai:{inquiry}:member:1:active");
        var fresh=adapter.acquire(1L,1L);
        old.complete(); old.close();
        assertThatThrownBy(() -> adapter.acquire(1L,1L)).isInstanceOf(InquiryException.class);
        fresh.complete(); fresh.close();
        assertThat(redis.opsForValue().get("ai:{inquiry}:2026-11-13:global:budget")).isEqualTo(String.valueOf(InquiryAdmissionPort.RESERVATION));
    }
}
