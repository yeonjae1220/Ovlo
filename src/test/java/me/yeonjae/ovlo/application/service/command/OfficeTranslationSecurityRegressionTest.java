package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.port.out.office.*;
import me.yeonjae.ovlo.application.port.out.chat.LoadChatPort;
import me.yeonjae.ovlo.domain.office.model.*;
import me.yeonjae.ovlo.domain.chat.model.*;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Regressions for cumulative translation spending and empty provider/cache results. */
class OfficeTranslationSecurityRegressionTest {
    @Test void repeatedPollsCannotBypassCumulativeAdmission() {
        var admitted=new java.util.concurrent.atomic.AtomicInteger();
        TranslationAdmissionPort limit=(office,member)->{
            assertThat(office).isEqualTo(5L); assertThat(member).isEqualTo(42L);
            if(admitted.incrementAndGet()>7) throw new IllegalStateException("budget exhausted");
            return ()->{};
        };
        var fixture=new Fixture(request->new TranslationPort.Result("message","번역","synthetic"),20,limit);
        for(int poll=0;poll<4;poll++) fixture.service.transcript(5L,700L,11L,true,NoticeLang.KO,100);
        assertThat(fixture.calls).hasSize(7); assertThat(fixture.saved).hasSize(7);
    }
    @Test void emptyLegacyCacheIsRepairedAndValidCacheCostsNothing() {
        var fixture=new Fixture(request->new TranslationPort.Result("message","repaired","synthetic"),1);
        fixture.saved.put(1L,"  ");
        var first=fixture.service.transcript(5L,700L,11L,true,NoticeLang.KO,100);
        assertThat(first.getFirst().translatedText()).isEqualTo("repaired");
        assertThat(first.getFirst().translationFailed()).isFalse();
        fixture.service.transcript(5L,700L,11L,true,NoticeLang.KO,100);
        assertThat(fixture.calls).hasSize(1);
    }
    @Test void alternateProviderBlankBodyIsNeverCached() {
        var fixture=new Fixture(request->new TranslationPort.Result("message"," ","synthetic"),1);
        assertThat(fixture.service.transcript(5L,700L,11L,true,NoticeLang.KO,100).getFirst().translationFailed()).isTrue();
        assertThat(fixture.saved).isEmpty();
    }

    @Test void emptyCompletedProviderTranslationIsRejectedAndCanBeRetried() throws Exception {
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{
            exchange.getRequestBody().readAllBytes();
            byte[] response="""
                {"id":"msg_test","type":"message","role":"assistant","model":"claude-opus-5-5",
                 "content":[{"type":"text","text":"TITLE: Message\\nBODY:"}],
                 "stop_reason":"end_turn","stop_sequence":null,"usage":{"input_tokens":10,"output_tokens":5}}
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type","application/json");
            exchange.sendResponseHeaders(200,response.length);
            exchange.getResponseBody().write(response);exchange.close();
        });
        server.start();
        try {
            var adapter=new me.yeonjae.ovlo.adapter.out.office.AnthropicLlmAdapter(
                new me.yeonjae.ovlo.shared.metrics.OfficeMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()),
                "sk-synthetic","http://127.0.0.1:"+server.getAddress().getPort(),"claude-opus-5-5","medium",2048,10,false);
            var fixture=new Fixture(adapter,1);
            var first=fixture.service.transcript(5L,700L,11L,true,NoticeLang.KO,100);
            var second=fixture.service.transcript(5L,700L,11L,true,NoticeLang.KO,100);
            assertThat(first.getFirst().translatedText()).isNull();
            assertThat(first.getFirst().translationFailed()).isTrue();
            assertThat(second.getFirst().translatedText()).isNull();
            assertThat(second.getFirst().translationFailed()).isTrue();
            assertThat(fixture.saved).isEmpty();
            assertThat(fixture.calls).hasSize(2);
        } finally {server.stop(0);}
    }

    static class Fixture {
        final List<String> calls=new ArrayList<>();
        final Map<Long,String> saved=new HashMap<>();
        final ConsultationTranscriptService service;
        Fixture(TranslationPort provider,int count) { this(provider,count,me.yeonjae.ovlo.support.TranslationAdmissionFixtures.available()); }
        Fixture(TranslationPort provider,int count,TranslationAdmissionPort admission) {
            var consultations=mock(LoadConsultationPort.class);var chat=mock(LoadChatPort.class);var cache=mock(MessageTranslationPort.class);
            var now=Instant.parse("2026-11-15T00:00:00Z");
            var c=Consultation.open(5L,42L,null,"v1",new Consultation.SharedContext("Visa?",null,NoticeLang.VI,true),300L,now);c.assignId(700L);
            when(consultations.findById(700L)).thenReturn(Optional.of(c));
            when(chat.findMessages(any(),eq(0),anyInt())).thenReturn(java.util.stream.LongStream.rangeClosed(1,count)
                .mapToObj(i->Message.restore(new MessageId(i),new MemberId(42L),"synthetic message "+i,now)).toList());
            when(cache.findTranslations(anyList(),eq(NoticeLang.KO))).thenAnswer(inv->Map.copyOf(saved));
            doAnswer(inv->{saved.put(inv.getArgument(0),inv.getArgument(2));return null;}).when(cache).save(any(),any(),any(),any());
            service=new ConsultationTranscriptService(consultations,chat,cache,request->{calls.add(request.body());return provider.translate(request);},
                me.yeonjae.ovlo.support.TranslationWorkFixtures.available(), admission, mock(me.yeonjae.ovlo.application.service.office.ConsultationTranscriptAccess.class));
        }
    }
}
