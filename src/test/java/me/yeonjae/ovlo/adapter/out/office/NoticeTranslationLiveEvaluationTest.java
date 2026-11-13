package me.yeonjae.ovlo.adapter.out.office;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import me.yeonjae.ovlo.application.port.out.office.TranslationPort;
import me.yeonjae.ovlo.application.service.office.NoticeTranslationGate;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.shared.metrics.OfficeMetrics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Paid, opt-in evaluation using synthetic notices; never writes production notices or student data. */
@Tag("live-llm")
@EnabledIfEnvironmentVariable(named = "OVLO_LIVE_LLM", matches = "1")
class NoticeTranslationLiveEvaluationTest {
    private static final List<Map<String, Object>> RESULTS = new ArrayList<>();
    private static AnthropicLlmAdapter llm;

    @BeforeAll static void setup() {
        llm = new AnthropicLlmAdapter(new OfficeMetrics(new SimpleMeterRegistry()), System.getenv("ANTHROPIC_API_KEY"),
                System.getenv().getOrDefault("OVLO_LLM_BASE_URL", "https://api.bizrouter.ai"),
                "claude-opus-5-5", "medium", 2048, 90, false, "claude-sonnet-5-5");
    }

    @ParameterizedTest
    @EnumSource(value = NoticeLang.class, names = {"EN", "DE", "FR", "JA", "ZH", "VI"})
    void preservesDecimalValuesAndRejectsCorruptedRealOutput(NoticeLang target) {
        var request = new TranslationPort.Request(NoticeLang.KO, target, "기숙사 신청 안내",
                "신청 마감은 2026년 11월 20일 18:00입니다. 안내 교육은 1.5시간입니다. "
                        + "보증금은 1,500.25유로입니다. 지연 수수료는 0.05%입니다. 정원은 15명입니다.");
        var gate = new NoticeTranslationGate();
        long started = System.nanoTime();
        var result = llm.translate(request);
        var originalVerdict = gate.check(request, result);
        String corruptedBody = result.body().replaceAll("(?<![0-9])1[.,]5(?![0-9])", "15");
        var corrupted = new TranslationPort.Result(result.title(), corruptedBody, result.modelId());
        var corruptedVerdict = gate.check(request, corrupted);
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("language", target.code());
        evidence.put("model", result.modelId());
        evidence.put("title", result.title());
        evidence.put("body", result.body());
        evidence.put("originalPassed", originalVerdict.passed());
        evidence.put("originalReasons", originalVerdict.reasons());
        evidence.put("decimalLossMutationApplied", !corruptedBody.equals(result.body()));
        evidence.put("decimalLossRejected", !corruptedVerdict.passed());
        evidence.put("decimalLossReasons", corruptedVerdict.reasons());
        evidence.put("latencyMs", (System.nanoTime() - started) / 1_000_000);
        RESULTS.add(evidence);
        assertThat(result.modelId()).contains("opus");
        assertThat(originalVerdict.passed()).as("%s: %s", target, originalVerdict.reasons()).isTrue();
        assertThat(corruptedBody).as("decimal-loss mutation must actually change the output").isNotEqualTo(result.body());
        assertThat(corruptedVerdict.passed()).isFalse();
    }

    @AfterAll static void writeEvidence() throws Exception {
        Path path = Path.of("build/reports/live-notice-numbers/results.json");
        Files.createDirectories(path.getParent());
        var evidence = Map.of("executedAt", Instant.now().toString(), "expectedLanguages", List.of("en", "de", "fr", "ja", "zh", "vi"),
                "results", RESULTS);
        Files.writeString(path, tools.jackson.databind.json.JsonMapper.builder().build()
                .writerWithDefaultPrettyPrinter().writeValueAsString(evidence));
        assertThat(RESULTS).hasSize(6);
    }
}
