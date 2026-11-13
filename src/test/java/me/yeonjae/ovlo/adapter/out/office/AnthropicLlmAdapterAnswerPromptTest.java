package me.yeonjae.ovlo.adapter.out.office;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import me.yeonjae.ovlo.application.port.out.office.AnswerGenerationPort;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.shared.metrics.OfficeMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 2026-11-02 운영 스모크에서 학생 화면(/my-university/ask)에 {@code **within 90 days** [1]} 처럼 마크다운이 날것으로 보였다.
 * 답변은 학생·직원 화면과 CSV 에서 모두 평문으로 쓰이므로, 답변 지침이 마크다운 강조를 금지하는지 실제 요청 본문으로 고정한다.
 */
class AnthropicLlmAdapterAnswerPromptTest {

    private static final String RESPONSE = """
            {"id":"msg_1","type":"message","role":"assistant","model":"anthropic/claude-sonnet-5.5",
             "content":[{"type":"text","text":"Apply within 90 days of entry. [1]"}],
             "stop_reason":"end_turn","stop_sequence":null,
             "usage":{"input_tokens":10,"output_tokens":5}}
            """;

    private HttpServer server;
    private final AtomicReference<String> body = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
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

    @Test
    @DisplayName("답변 지침은 평문만 쓰고 마크다운 강조를 금지하며 '- ' 목록은 허용한다")
    void answerSystemForbidsMarkdownEmphasis() {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        AnthropicLlmAdapter adapter = new AnthropicLlmAdapter(new OfficeMetrics(new SimpleMeterRegistry()), "sk-test",
                baseUrl, "claude-opus-5-5", "medium", 256, 10, false);

        AnswerGenerationPort.Result result = adapter.answer(new AnswerGenerationPort.Request(
                "When do I register?", NoticeLang.EN,
                List.of(new AnswerGenerationPort.Candidate(1, 10L, "Alien registration", "Within 90 days.", null))));

        assertThat(result.text()).isEqualTo("Apply within 90 days of entry. [1]");
        assertThat(body.get())
                .contains("Write plain text only")
                .contains("Do not use markdown")
                .contains("emphasis (** or __ or *)")
                .contains("start with \\\"- \\\" is fine");
    }

    @Test void historyIsContextOnlyAndOldCitationsAreNotEvidence() {
        AnthropicLlmAdapter adapter = new AnthropicLlmAdapter(new OfficeMetrics(new SimpleMeterRegistry()), "sk-test",
                "http://127.0.0.1:" + server.getAddress().getPort(), "claude-opus-5-5", "medium", 256, 10, false);
        adapter.answer(new AnswerGenerationPort.Request("Where?", NoticeLang.EN,
                List.of(new AnswerGenerationPort.Candidate(1, 10L, "Dorm", "Apply online", null)),
                List.of(new AnswerGenerationPort.Turn("When does dorm registration close?", "Old answer [9]"))));
        assertThat(body.get()).contains("When does dorm registration close?", "Old answer [9]", "untrusted context ONLY",
                "CURRENT numbered reference notices", "NO_GROUNDS");
    }

}
