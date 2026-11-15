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

    @Test void failuresBackOffAndStopAfterThreeAttemptsInA24HourWindow() {
        String key="test:"+UUID.randomUUID(), retry="office:translation:retry:"+key;
        for(int attempt=1;attempt<=3;attempt++) {
            if(attempt>1) redis.opsForHash().put(retry,"nextAt","0"); // advance retry eligibility without sleeping
            try(var lease=sut.tryAcquire(key).orElseThrow()) {
                assertThat(lease.tryBeginAttempt()).isTrue();
                assertThat(lease.tryBeginAttempt()).isFalse();
                lease.translationFailed();
            }
            assertThat(redis.opsForHash().get(retry,"attempts")).isEqualTo(""+attempt);
            assertThat(redis.getExpire(retry)).isBetween(1L,86400L);
            long remaining=Long.parseLong((String)redis.opsForHash().get(retry,"nextAt"))-System.currentTimeMillis();
            assertThat(remaining).isBetween(attempt==1?50000L:290000L,attempt==1?61000L:301000L);
            try(var immediate=sut.tryAcquire(key).orElseThrow()) { assertThat(immediate.tryBeginAttempt()).isFalse(); }
        }
        redis.opsForHash().put(retry,"nextAt","0");
        try(var exhausted=sut.tryAcquire(key).orElseThrow()) { assertThat(exhausted.tryBeginAttempt()).isFalse(); }
        // Window expiry permits recovery; success removes retry metadata.
        redis.expire(retry,Duration.ZERO);
        try(var recovered=sut.tryAcquire(key).orElseThrow()) {
            assertThat(recovered.tryBeginAttempt()).isTrue();
            recovered.translationSucceeded();
            assertThat(redis.hasKey(retry)).isFalse();
        }
    }

    @Test void lostWorkersCannotResetOrDelayReplacementRetryState() {
        String key="test:"+UUID.randomUUID(), retry="office:translation:retry:"+key;
        var old=sut.tryAcquire(key).orElseThrow();
        assertThat(old.tryBeginAttempt()).isTrue();
        redis.expire("office:translation:"+key,Duration.ZERO);
        redis.opsForHash().put(retry,"nextAt","0");
        try(var replacement=sut.tryAcquire(key).orElseThrow()) {
            assertThat(replacement.tryBeginAttempt()).isTrue();
            var before=redis.opsForHash().entries(retry);
            old.translationFailed();old.translationSucceeded();old.close();
            assertThat(redis.opsForHash().entries(retry)).isEqualTo(before);
            assertThat(replacement.isHeld()).isTrue();
        }
    }

    @Test void failedFirstFiveDoNotStarveSixthMessageOnNextPoll() throws Exception {
        verifyProgress(request -> new me.yeonjae.ovlo.application.port.out.office.TranslationPort.Result("message","내일 여권을 가져오세요.","test"),false);
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="OVLO_LIVE_LLM",matches="1")
    void liveTranslationProgressesPastFiveControlledFailures() throws Exception {
        var llm=new me.yeonjae.ovlo.adapter.out.office.AnthropicLlmAdapter(
            new me.yeonjae.ovlo.shared.metrics.OfficeMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()),
            System.getenv("ANTHROPIC_API_KEY"),System.getenv().getOrDefault("OVLO_LLM_BASE_URL","https://api.bizrouter.ai"),
            "claude-opus-5-5","medium",2048,90,false,"claude-sonnet-5-5");
        verifyProgress(llm,true);
    }

    private void verifyProgress(me.yeonjae.ovlo.application.port.out.office.TranslationPort healthy,boolean live) throws Exception {
        var consultations=org.mockito.Mockito.mock(me.yeonjae.ovlo.application.port.out.office.LoadConsultationPort.class);
        var chat=org.mockito.Mockito.mock(me.yeonjae.ovlo.application.port.out.chat.LoadChatPort.class);
        var cache=org.mockito.Mockito.mock(me.yeonjae.ovlo.application.port.out.office.MessageTranslationPort.class);
        var now=java.time.Instant.now();
        var lang=me.yeonjae.ovlo.domain.office.model.NoticeLang.KO;
        var c=me.yeonjae.ovlo.domain.office.model.Consultation.open(5L,42L,null,"v1",
            new me.yeonjae.ovlo.domain.office.model.Consultation.SharedContext("Visa?",null,me.yeonjae.ovlo.domain.office.model.NoticeLang.VI,true),300L,now);
        c.assignId(700L);
        org.mockito.Mockito.when(consultations.findById(700L)).thenReturn(java.util.Optional.of(c));
        long base=Integer.toUnsignedLong(UUID.randomUUID().hashCode())*10;
        var messages=java.util.stream.LongStream.rangeClosed(1,6).mapToObj(i ->
            me.yeonjae.ovlo.domain.chat.model.Message.restore(new me.yeonjae.ovlo.domain.chat.model.MessageId(base+i),
                new me.yeonjae.ovlo.domain.member.model.MemberId(42L),i<6?"controlled failure "+i:"Vui lòng mang hộ chiếu đến văn phòng vào ngày mai.",now)).toList();
        org.mockito.Mockito.when(chat.findMessages(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.eq(0),org.mockito.ArgumentMatchers.anyInt())).thenReturn(messages);
        java.util.Map<Long,String> stored=new java.util.HashMap<>();
        org.mockito.Mockito.when(cache.findTranslations(org.mockito.ArgumentMatchers.anyList(),org.mockito.ArgumentMatchers.eq(lang)))
            .thenAnswer(inv->java.util.Map.copyOf(stored));
        org.mockito.Mockito.doAnswer(inv->{stored.put(inv.getArgument(0),inv.getArgument(2));return null;})
            .when(cache).save(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
        var attempted=new java.util.ArrayList<String>();
        var models=new java.util.ArrayList<String>();
        me.yeonjae.ovlo.application.port.out.office.TranslationPort provider=request->{
            attempted.add(request.body());
            if(request.body().startsWith("controlled")) throw new IllegalStateException("controlled failure");
            var result=healthy.translate(request);models.add(result.modelId());return result;
        };
        var service=new me.yeonjae.ovlo.application.service.command.ConsultationTranscriptService(consultations,chat,cache,provider,sut);
        var first=service.transcript(5L,700L,11L,true,lang,100);
        assertThat(attempted).hasSize(5);
        assertThat(first).allSatisfy(m->assertThat(m.translationFailed()).isTrue());
        var second=service.transcript(5L,700L,11L,true,lang,100);
        assertThat(attempted).hasSize(6);
        assertThat(second.getLast().translatedText()).isNotBlank();
        assertThat(second.getLast().translationFailed()).isFalse();
        assertThat(second.subList(0,5)).allSatisfy(m->{assertThat(m.translationFailed()).isTrue();assertThat(m.content()).startsWith("controlled");});
        service.transcript(5L,700L,11L,true,lang,100);
        assertThat(attempted).hasSize(6); // cooldown + saved successful translation
        if(live) {
            assertThat(models).allSatisfy(model->assertThat(model).contains("opus"));
            var file=java.nio.file.Path.of("build/reports/live-office-retry/results.json");
            java.nio.file.Files.createDirectories(file.getParent());
            java.nio.file.Files.writeString(file,tools.jackson.databind.json.JsonMapper.builder().build().writerWithDefaultPrettyPrinter()
                .writeValueAsString(java.util.Map.of("passed",true,"controlledFailures",5,"actualProviderCalls",1,
                    "polls",3,"models",models,"translatedText",second.getLast().translatedText())));
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
