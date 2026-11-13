package me.yeonjae.ovlo.adapter.out.office;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import me.yeonjae.ovlo.shared.metrics.OfficeMetrics;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;

class AnthropicMarketplaceTranslationTest {
    private HttpServer server;
    private String stopReason="end_turn";
    private AnthropicLlmAdapter adapter;
    @BeforeEach void setup() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{
            exchange.getRequestBody().readAllBytes();
            byte[] response=("""
                {"id":"msg_test","type":"message","role":"assistant","model":"claude-opus-5-5",
                "content":[{"type":"text","text":"TITLE: Kitchen bundle BODY: Pots only. Microwave excluded. 25000 KRW."}],
                "stop_reason":"%s","stop_sequence":null,"usage":{"input_tokens":100,"output_tokens":2048}}
                """).formatted(stopReason).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type","application/json");
            exchange.sendResponseHeaders(200,response.length);
            exchange.getResponseBody().write(response);exchange.close();
        });
        server.start();
        adapter=new AnthropicLlmAdapter(new OfficeMetrics(new SimpleMeterRegistry()),"sk-test",
                "http://127.0.0.1:"+server.getAddress().getPort(),"claude-opus-5-5","medium",2048,10,false);
    }
    @AfterEach void close() {server.stop(0);}
    @Test void completeTranslationPreservesTerms() {
        var result=adapter.translateListing("ko","en","주방 묶음","냄비만 판매. 전자레인지 제외. 25000 KRW.");
        assertThat(result.title()).isEqualTo("Kitchen bundle");
        assertThat(result.description()).isEqualTo("Pots only. Microwave excluded. 25000 KRW.");
    }
    @ParameterizedTest @ValueSource(strings={"max_tokens","pause_turn","stop_sequence","refusal","future_reason"})
    void nonFinalTranslationCannotBeAcceptedEvenWithValidTitleAndBody(String reason) {
        stopReason=reason;
        assertThatThrownBy(()->adapter.translateListing("ko","en","제목","본문"))
                .isInstanceOf(IllegalStateException.class);
    }
}
