package me.yeonjae.ovlo.adapter.out.redis;

import me.yeonjae.ovlo.application.port.out.office.TranslationAdmissionPort;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@Tag("integration") @Testcontainers
class RedisTranslationAdmissionAdapterIntegrationTest {
    @Container static GenericContainer<?> container=new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    static LettuceConnectionFactory connection;
    static StringRedisTemplate redis;
    static final long R=TranslationAdmissionPort.RESERVATION;
    @BeforeAll static void connect() {
        connection=new LettuceConnectionFactory(container.getHost(),container.getMappedPort(6379));
        connection.afterPropertiesSet(); connection.start(); redis=new StringRedisTemplate(connection);
    }
    @AfterAll static void disconnect() { connection.destroy(); }
    @BeforeEach void clear() { try(var c=connection.getConnection()) { c.serverCommands().flushAll(); } }
    RedisTranslationAdmissionAdapter adapter(int member,int office,int global,long mb,long ob,long gb,int oc,int gc) {
        return new RedisTranslationAdmissionAdapter(redis,member,office,global,mb,ob,gb,oc,gc);
    }
    @Test void fourPollsOfNewMessagesCannotExceedSevenCallsEvenAcrossSchools() {
        var sut=adapter(7,100,100,100*R,100*R,100*R,40,40);
        int calls=0;
        for(int i=0;i<20;i++) {
            try(var lease=sut.acquire((long)(i%2+1),42L)) { calls++; } catch(IllegalStateException denied) { }
        }
        assertThat(calls).isEqualTo(7);
        assertThat(redis.opsForHash().get("ai:{translation}:member:42:daily","count")).isEqualTo("7");
        assertThat(redis.opsForHash().get("ai:{translation}:global:daily","budget")).isEqualTo(""+(7*R));
    }
    @Test void twentyConcurrentRequestsCannotOvershootLastOfficeSlot() throws Exception {
        var sut=adapter(100,1,100,100*R,100*R,100*R,40,40);
        var barrier=new CyclicBarrier(20);
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            var futures=new ArrayList<Future<Boolean>>();
            for(long id=1;id<=20;id++) { long member=id;
                futures.add(pool.submit(()->{barrier.await();try(var lease=sut.acquire(1L,member)){return true;}catch(IllegalStateException denied){return false;}}));
            }
            int accepted=0;for(var f:futures)if(f.get())accepted++;
            assertThat(accepted).isEqualTo(1);
        }
    }
    @Test void cumulativeBudgetAppliesToEachScopeAndCannotBeRefundedByClose() {
        for(int scope=0;scope<3;scope++) {
            clear();var sut=adapter(100,100,100,scope==0?R:100*R,scope==1?R:100*R,scope==2?R:100*R,40,40);
            var lease=sut.acquire(1L,1L);lease.close();lease.close();
            long office=scope==2?2L:1L, member=scope==0?1L:2L;
            assertThatThrownBy(()->sut.acquire(office,member)).isInstanceOf(IllegalStateException.class);
            assertThat(redis.opsForHash().get("ai:{translation}:global:daily","budget")).isEqualTo(""+R);
        }
    }
    @Test void memberOfficeAndGlobalConcurrencyAreIndependentOfDailyCounters() {
        var sut=adapter(100,100,100,100*R,100*R,100*R,1,2);
        try(var first=sut.acquire(1L,1L)) {
            assertThatThrownBy(()->sut.acquire(2L,1L)).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(()->sut.acquire(1L,2L)).isInstanceOf(IllegalStateException.class);
            try(var second=sut.acquire(2L,2L)) {
                assertThatThrownBy(()->sut.acquire(3L,3L)).isInstanceOf(IllegalStateException.class);
            }
            try(var third=sut.acquire(3L,3L)) { }
        }
    }
    @Test void expiredOwnerCannotReleaseReplacementAndNewUtcDayResetsBudget() {
        var sut=adapter(100,100,100,100*R,100*R,100*R,5,20);
        var old=sut.acquire(1L,1L);
        for(String scope:List.of("member:1","office:1","global")) redis.delete("ai:{translation}:"+scope+":active");
        var fresh=sut.acquire(1L,1L);old.close();
        assertThatThrownBy(()->sut.acquire(1L,1L)).isInstanceOf(IllegalStateException.class);fresh.close();
        for(String scope:List.of("member:1","office:1","global")) redis.opsForHash().put("ai:{translation}:"+scope+":daily","day","-1");
        try(var next=sut.acquire(1L,1L)) { }
        assertThat(redis.opsForHash().get("ai:{translation}:global:daily","count")).isEqualTo("1");
    }
    @Test void redisFailureFailsClosed() {
        var broken=org.mockito.Mockito.mock(StringRedisTemplate.class);
        var sut=new RedisTranslationAdmissionAdapter(broken,100,100,100,R,R,R,5,20);
        assertThatThrownBy(()->sut.acquire(1L,1L)).isInstanceOf(IllegalStateException.class);
    }

    @Test void noticeReservesTranslationAndVerificationTogetherWithLongerLease() {
        var sut=adapter(100,100,100,2*R,100*R,100*R,5,20);
        try(var lease=sut.acquire(1L,1L,2)) {
            assertThat(redis.opsForHash().get("ai:{translation}:member:1:daily","budget")).isEqualTo(""+(2*R));
            var entries=redis.opsForZSet().rangeWithScores("ai:{translation}:member:1:active",0,-1);
            assertThat(entries).hasSize(1);
            assertThat(entries.iterator().next().getScore()).isGreaterThan((double)java.time.Instant.now().getEpochSecond()+270);
            assertThatThrownBy(()->sut.acquire(2L,1L)).isInstanceOf(TranslationAdmissionPort.Unavailable.class)
                    .satisfies(e->assertThat(((TranslationAdmissionPort.Unavailable)e).retryAfterSeconds()).isEqualTo(30));
        }
        assertThatThrownBy(()->sut.acquire(1L,1L)).isInstanceOf(TranslationAdmissionPort.Unavailable.class)
                .satisfies(e->assertThat(((TranslationAdmissionPort.Unavailable)e).retryAfterSeconds()).isBetween(1L,86401L));
    }

}