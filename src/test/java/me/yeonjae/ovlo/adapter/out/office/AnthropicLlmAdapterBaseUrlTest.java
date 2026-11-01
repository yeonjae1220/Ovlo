package me.yeonjae.ovlo.adapter.out.office;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import me.yeonjae.ovlo.application.port.out.office.TranslationPort;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.shared.metrics.OfficeMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 2026-11-01 운영 키가 BizRouter(Messages 호환 게이트웨이) 발급이라 api.anthropic.com 으로 보내면 401 이었다.
 * base-url 을 넣으면 그 주소로 보내고, 게이트웨이가 거절하는 fallbacks 는 꺼진 설정에서 보내지 않는다.
 */
class AnthropicLlmAdapterBaseUrlTest {

    private static final String RESPONSE = """
            {"id":"msg_1","type":"message","role":"assistant","model":"anthropic/claude-opus-5.5",
             "content":[{"type":"text","text":"TITLE: Hello\\nBODY:\\nWorld"}],
             "stop_reason":"end_turn","stop_sequence":null,
             "usage":{"input_tokens":10,"output_tokens":5}}
            """;

    private HttpServer server;
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> apiKeyHeader = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            apiKeyHeader.set(exchange.getRequestHeaders().getFirst("x-api-key"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = RESPONSE.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private AnthropicLlmAdapter adapter(boolean serverSideFallback) {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        return new AnthropicLlmAdapter(new OfficeMetrics(new SimpleMeterRegistry()), "sk-br-test", baseUrl,
                "claude-opus-5-5", "medium", 256, 10, serverSideFallback);
    }

    @Test
    @DisplayName("base-url 을 넣으면 그 게이트웨이의 /v1/messages 로 키를 실어 보낸다")
    void sendsToConfiguredBaseUrl() {
        TranslationPort.Result result = adapter(false)
                .translate(new TranslationPort.Request(NoticeLang.KO, NoticeLang.EN, "안녕", "세상"));

        assertThat(path.get()).isEqualTo("/v1/messages");
        assertThat(apiKeyHeader.get()).isEqualTo("sk-br-test");
        assertThat(result.title()).isEqualTo("Hello");
        assertThat(result.body()).isEqualTo("World");
        assertThat(result.modelId()).isEqualTo("anthropic/claude-opus-5.5");
    }

    @Test
    @DisplayName("서버측 폴백을 끄면 fallbacks 를 보내지 않고 effort 는 그대로 보낸다")
    void omitsFallbacksWhenDisabled() {
        adapter(false).translate(new TranslationPort.Request(NoticeLang.KO, NoticeLang.EN, "안녕", "세상"));

        assertThat(body.get()).doesNotContain("\"fallbacks\"");
        assertThat(body.get()).contains("\"output_config\"").contains("\"effort\":\"medium\"");
    }

    @Test
    @DisplayName("서버측 폴백을 켜면 fallbacks 를 보낸다 (Anthropic 직결용)")
    void sendsFallbacksWhenEnabled() {
        adapter(true).translate(new TranslationPort.Request(NoticeLang.KO, NoticeLang.EN, "안녕", "세상"));

        assertThat(body.get()).contains("\"fallbacks\":\"default\"");
    }
}
