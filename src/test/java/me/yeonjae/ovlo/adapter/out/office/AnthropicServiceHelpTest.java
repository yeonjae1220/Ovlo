package me.yeonjae.ovlo.adapter.out.office;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import me.yeonjae.ovlo.application.port.out.help.ServiceHelpLlmPort;
import me.yeonjae.ovlo.domain.office.exception.InquiryException;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.shared.metrics.OfficeMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 서비스 도움말은 국제처 프롬프트가 아니라 자기 프롬프트를 쓰고, 같은 비용·실패 닫힘 규칙을 따른다. */
class AnthropicServiceHelpTest {
    private HttpServer server;
    private final AtomicReference<String> reply = new AtomicReference<>("{\"decision\":\"ALLOW\"}");
    private final AtomicReference<String> sent = new AtomicReference<>();
    private final AtomicInteger calls = new AtomicInteger();
    private final tools.jackson.databind.json.JsonMapper json = tools.jackson.databind.json.JsonMapper.builder().build();
    private AnthropicLlmAdapter adapter;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            sent.set(body);
            String model = json.readTree(body).get("model").asText();
            byte[] bytes = json.writeValueAsBytes(Map.of("id", "msg_test", "type", "message", "role", "assistant",
                    "model", model, "content", List.of(Map.of("type", "text", "text", reply.get())),
                    "stop_reason", "end_turn", "usage", Map.of("input_tokens", 10, "output_tokens", 2)));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        adapter = new AnthropicLlmAdapter(new OfficeMetrics(new SimpleMeterRegistry()), "test-only",
                "http://127.0.0.1:" + server.getAddress().getPort(), "claude-opus-5-5", "medium", 2048, 10, true,
                "claude-sonnet-5-5");
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private static ServiceHelpLlmPort.Request request() {
        return new ServiceHelpLlmPort.Request("How do I delete my account?", NoticeLang.EN, List.of(
                new ServiceHelpLlmPort.Reference(1, "회원 탈퇴", "프로필 설정에서 탈퇴합니다."),
                new ServiceHelpLlmPort.Reference(2, "차단", "메뉴에서 차단합니다.")));
    }

    @Test
    @DisplayName("분류는 도움말 전용 프롬프트·Sonnet·JSON 스키마를 쓰고 폴백을 붙이지 않는다")
    void screen_usesHelpPromptWithStrictSchema() {
        assertThat(adapter.screenHelp("How do I block someone?").allowed()).isTrue();

        var body = json.readTree(sent.get());
        assertThat(body.get("model").asText()).isEqualTo("claude-sonnet-5-5");
        assertThat(body.has("fallbacks")).isFalse();
        assertThat(body.get("output_config").get("format").get("type").asText()).isEqualTo("json_schema");
        assertThat(body.get("system").asText()).contains("Ovlo", "untrusted DATA").doesNotContain("international-office request");
    }

    @Test
    @DisplayName("분류 응답이 형식에 맞지 않으면 실패로 닫는다")
    void screen_malformedDecisionFailsClosed() {
        reply.set("ALLOW");
        assertThatThrownBy(() -> adapter.screenHelp("탈퇴?")).isInstanceOf(InquiryException.class);
    }

    @Test
    @DisplayName("답변 요청에는 문서 번호·제목·본문만 들어가고 출력 형식 제약은 없다")
    void answer_sendsNumberedArticlesAsData() {
        reply.set("Open profile settings. [1]");

        var answer = adapter.answerHelp(request());

        assertThat(answer.text()).isEqualTo("Open profile settings. [1]");
        var body = json.readTree(sent.get());
        assertThat(body.get("model").asText()).isEqualTo("claude-sonnet-5-5");
        assertThat(body.get("output_config").has("format")).isFalse();
        String user = body.get("messages").get(0).get("content").asText();
        assertThat(user).contains("Answer language: English", "\"citation\":\"[1]\"", "회원 탈퇴");
        assertThat(body.get("system").asText()).contains("help articles", "NO_GROUNDS");
    }

    @Test
    @DisplayName("검증은 인용한 문서만 보고, 인용이 없으면 호출하지 않고 실패로 닫는다")
    void verify_seesOnlyCitedArticles() {
        reply.set("{\"decision\":\"DENY\"}");
        assertThat(adapter.verifyHelp(request(), "Use the block menu. [2]").allowed()).isFalse();
        String user = json.readTree(sent.get()).get("messages").get(0).get("content").asText();
        assertThat(user).contains("차단").doesNotContain("회원 탈퇴");

        int before = calls.get();
        assertThatThrownBy(() -> adapter.verifyHelp(request(), "No citation here"))
                .isInstanceOf(InquiryException.class);
        assertThat(calls.get()).isEqualTo(before);
    }
}
