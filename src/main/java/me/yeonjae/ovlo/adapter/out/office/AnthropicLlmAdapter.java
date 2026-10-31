package me.yeonjae.ovlo.adapter.out.office;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.MessageCreateParams;
import me.yeonjae.ovlo.application.port.out.office.AnswerGenerationPort;
import me.yeonjae.ovlo.application.port.out.office.TranslationPort;
import me.yeonjae.ovlo.application.service.office.GroundedAnswerValidator;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Anthropic Claude 로 근거 기반 답변과 공지 번역을 만든다(설계 §8). {@code ovlo.llm.provider=anthropic} 일 때만 활성.
 *
 * - 학생 질문은 신뢰하지 않는 입력이다: 지침은 system, 후보 자료와 질문은 user 에 넣고 도구는 주지 않는다.
 * - 모델이 지시를 어겼는지는 여기서 판단하지 않는다(GroundedAnswerValidator 가 최종 게이트).
 * - 안전 분류기 거부(stop_reason=refusal)는 {@code refused=true} 로 돌려준다. 서버측 폴백(fallbacks: "default")을 켜 두어
 *   거부 분류에 따라 다른 모델로 한 번 더 시도한다 — 이 동작은 운영 설정에서 끌 수 있다.
 * - 호출마다 모델·입출력 토큰을 로그로 남긴다. 실패는 예외로 전파한다(빈 값 치환 금지).
 */
@Component
@ConditionalOnProperty(name = "ovlo.llm.provider", havingValue = "anthropic")
public class AnthropicLlmAdapter implements AnswerGenerationPort, TranslationPort {

    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmAdapter.class);

    private static final String ANSWER_SYSTEM = """
            You answer questions from international students at a Korean university on behalf of the university's
            international office. You will receive numbered reference notices approved by the office, then the student's question.

            Rules:
            1. Use ONLY the content of the reference notices. Do not add outside knowledge or assumptions.
            2. Write the answer in the language given as "Answer language". Use polite, plain wording.
            3. After each sentence that uses a notice, cite it as [N] using the notice number. Every factual sentence needs a citation.
            4. Copy dates, deadlines, amounts, durations and office hours exactly as written in the notices. Never convert or round them.
            5. Do not judge personal eligibility (visa status, whether a specific student qualifies). If the question needs an
               individual decision, say that the international office must decide and suggest contacting them.
            6. Do not include URLs or contact details that are not in the notices.
            7. If the notices do not contain the information needed, reply with exactly: NO_GROUNDS
            """;

    private static final String TRANSLATE_SYSTEM = """
            You translate official notices from a Korean university's international office for international students.
            Translate the title and body faithfully into the target language. Keep every number, date, time, amount, duration,
            email address and URL exactly as in the source. Do not add, omit or summarize. Keep line breaks.
            Reply in exactly this format and nothing else:
            TITLE: <translated title>
            BODY:
            <translated body>
            """;

    private final AnthropicClient client;
    private final String model;
    private final String effort;
    private final long maxTokens;
    private final boolean serverSideFallback;

    private final me.yeonjae.ovlo.shared.metrics.OfficeMetrics metrics;

    public AnthropicLlmAdapter(me.yeonjae.ovlo.shared.metrics.OfficeMetrics metrics,
                               @Value("${ovlo.llm.anthropic.api-key:}") String apiKey,
                               @Value("${ovlo.llm.anthropic.model:claude-opus-5-5}") String model,
                               @Value("${ovlo.llm.anthropic.effort:medium}") String effort,
                               @Value("${ovlo.llm.anthropic.max-tokens:2048}") long maxTokens,
                               @Value("${ovlo.llm.anthropic.timeout-seconds:90}") long timeoutSeconds,
                               @Value("${ovlo.llm.anthropic.server-side-fallback:true}") boolean serverSideFallback) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("ovlo.llm.provider=anthropic 이면 ANTHROPIC_API_KEY 가 필요합니다");
        }
        this.client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        this.metrics = metrics;
        this.model = model;
        this.effort = effort;
        this.maxTokens = maxTokens;
        this.serverSideFallback = serverSideFallback;
        log.info("[LLM] Anthropic adapter model={} effort={} maxTokens={} fallback={}", model, effort, maxTokens, serverSideFallback);
    }

    // ── AnswerGenerationPort ─────────────────────────────────────────────────

    @Override
    public AnswerGenerationPort.Result answer(AnswerGenerationPort.Request request) {
        String references = request.candidates().stream()
                .map(c -> "[" + c.index() + "] " + c.title() + "\n" + c.body())
                .collect(Collectors.joining("\n\n---\n\n"));
        String user = "Answer language: " + languageName(request.lang()) + "\n\n"
                + "<reference_notices>\n" + references + "\n</reference_notices>\n\n"
                + "<student_question>\n" + request.question() + "\n</student_question>";

        Completion c = complete(ANSWER_SYSTEM, user, "answer");
        return new AnswerGenerationPort.Result(c.refused ? "" : c.text, c.modelId, c.inputTokens, c.outputTokens, c.refused);
    }

    // ── TranslationPort ──────────────────────────────────────────────────────

    @Override
    public TranslationPort.Result translate(TranslationPort.Request request) {
        String user = "Source language: " + languageName(request.from()) + "\n"
                + "Target language: " + languageName(request.to()) + "\n\n"
                + "TITLE: " + request.title() + "\nBODY:\n" + request.body();
        Completion c = complete(TRANSLATE_SYSTEM, user, "translate");
        if (c.refused) {
            throw new IllegalStateException("번역 요청이 거부되었습니다(stop_reason=refusal)");
        }
        String text = c.text.trim();
        int titleAt = text.indexOf("TITLE:");
        int bodyAt = text.indexOf("BODY:");
        if (titleAt < 0 || bodyAt < 0 || bodyAt < titleAt) {
            throw new IllegalStateException("번역 응답 형식이 맞지 않습니다");
        }
        String title = text.substring(titleAt + "TITLE:".length(), bodyAt).trim();
        String body = text.substring(bodyAt + "BODY:".length()).trim();
        return new TranslationPort.Result(title, body, c.modelId);
    }

    // ── 공통 ──────────────────────────────────────────────────────────────────

    private record Completion(String text, String modelId, long inputTokens, long outputTokens, boolean refused) {}

    private Completion complete(String system, String user, String purpose) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokens)
                .system(system)
                .addUserMessage(user)
                // 생각 깊이: thinking 은 항상 켜져 있고 effort 로만 조절한다 (Claude Opus 5.5)
                .putAdditionalBodyProperty("output_config", JsonValue.from(Map.of("effort", effort)));
        if (serverSideFallback) {
            builder.addBeta("server-side-fallback-2026-07-01")
                   .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
        }

        long started = System.nanoTime();
        BetaMessage message;
        try {
            message = client.beta().messages().create(builder.build());
        } catch (AnthropicException e) {
            log.warn("[LLM] {} failed model={}: {}", purpose, model, e.toString());
            metrics.llmCall(purpose, model, "error", 0, 0, (System.nanoTime() - started) / 1_000_000);
            throw e;
        }
        long ms = (System.nanoTime() - started) / 1_000_000;

        String stopReason = message.stopReason().map(Object::toString).orElse("");
        boolean refused = "refusal".equalsIgnoreCase(stopReason);
        String text = message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(t -> t.text())
                .collect(Collectors.joining());
        long in = message.usage().inputTokens();
        long out = message.usage().outputTokens();
        String usedModel = message.model().toString();
        log.info("[LLM] {} model={} stop={} in={} out={} ms={}", purpose, usedModel, stopReason, in, out, ms);
        metrics.llmCall(purpose, usedModel, refused ? "refused" : "ok", in, out, ms);
        if (refused) {
            message.stopDetails().ifPresent(d -> log.warn("[LLM] {} refused: {}", purpose, d));
        }
        return new Completion(text, usedModel, in, out, refused);
    }

    static String languageName(NoticeLang lang) {
        return switch (lang) {
            case KO -> "Korean";
            case EN -> "English";
            case JA -> "Japanese";
            case ZH -> "Chinese (Simplified)";
            case DE -> "German";
            case FR -> "French";
            case VI -> "Vietnamese";
        };
    }

    /** 답변 없음 마커 — 프롬프트와 검증기가 같은 문자열을 쓴다. */
    static String noGroundsMarker() {
        return GroundedAnswerValidator.NO_GROUNDS_MARKER;
    }
}
