package me.yeonjae.ovlo.adapter.out.office;

import com.sun.net.httpserver.HttpServer;
import me.yeonjae.ovlo.application.port.out.office.*;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.InetSocketAddress;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** The actual SDK parses a refusal response containing synthetic private data; no provider detail may be logged. */
class AnthropicRefusalPrivacyTest {
    @ParameterizedTest @ValueSource(strings={"answer", "screen", "verify", "translate", "translation-audit"})
    void providerRefusalDetailsNeverReachApplicationLogs(String purpose) throws Exception {
        String privateMarker = "SYNTHETIC_PRIVATE_PASSPORT_123456";
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var json = tools.jackson.databind.json.JsonMapper.builder().build();
        server.createContext("/", exchange -> {
            String requestedModel = json.readTree(exchange.getRequestBody().readAllBytes()).get("model").asText();
            byte[] body = json.writeValueAsBytes(Map.of("id", "msg_test", "type", "message", "role", "assistant", "model", requestedModel,
                    "content", List.of(Map.of("type", "text", "text", privateMarker)), "stop_reason", "refusal",
                    "stop_details", Map.of("type", "refusal", "reason", privateMarker),
                    "usage", Map.of("input_tokens", 10, "output_tokens", 2)));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length); exchange.getResponseBody().write(body); exchange.close();
        });
        server.start();
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(AnthropicLlmAdapter.class);
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.setContext(logger.getLoggerContext()); appender.start(); logger.addAppender(appender);
        try {
            var adapter = new AnthropicLlmAdapter(new me.yeonjae.ovlo.shared.metrics.OfficeMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()),
                    "test-only", "http://127.0.0.1:" + server.getAddress().getPort(), "claude-opus-5-5", "medium", 2048, 10, false, "claude-sonnet-5-5");
            var translation = new TranslationPort.Request(NoticeLang.KO, NoticeLang.EN, "문의", privateMarker);
            var request = new AnswerGenerationPort.Request("Training?", NoticeLang.EN,
                    List.of(new AnswerGenerationPort.Candidate(1, 1L, "Training", privateMarker, null)));
            if (purpose.equals("answer")) {
                var result = adapter.answer(request);
                assertThat(result.refused()).isTrue(); assertThat(result.text()).isEmpty();
            } else {
                assertThatThrownBy(() -> {
                    switch (purpose) {
                        case "screen" -> adapter.screen(privateMarker, List.of());
                        case "verify" -> adapter.verify(request, "Duration [1]");
                        case "translate" -> adapter.translate(translation);
                        case "translation-audit" -> adapter.verifyTranslation(translation, new TranslationPort.Result("Query", privateMarker, "test"));
                        default -> throw new AssertionError(purpose);
                    }
                }).isInstanceOf(RuntimeException.class).hasMessageNotContaining(privateMarker);
            }
            assertThat(appender.list.stream().map(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage).toList().toString())
                    .doesNotContain(privateMarker).contains("stop=refusal");
        } finally {
            logger.detachAppender(appender); appender.stop(); server.stop(0);
        }
    }
}
