package me.yeonjae.ovlo.adapter.out.office;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import me.yeonjae.ovlo.application.port.out.office.TranslationPort;
import me.yeonjae.ovlo.application.service.office.NoticeTranslationGate;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.shared.metrics.OfficeMetrics;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** Paid opt-in evaluation; only synthetic school text is sent to the provider. */
@Tag("live-llm")
@EnabledIfEnvironmentVariable(named="OVLO_LIVE_LLM",matches="1")
class OfficeLocaleLiveEvaluationTest {
    static final List<Map<String,Object>> results=new ArrayList<>();
    @ParameterizedTest
    @EnumSource(value=NoticeLang.class,names={"DE","FR"})
    void realTranslationAndEquivalentLocalTimeNotationPass(NoticeLang language) {
        var llm=new AnthropicLlmAdapter(new OfficeMetrics(new SimpleMeterRegistry()),System.getenv("ANTHROPIC_API_KEY"),
            System.getenv().getOrDefault("OVLO_LLM_BASE_URL","https://api.bizrouter.ai"),
            "claude-opus-5-5","medium",2048,90,false,"claude-sonnet-5-5");
        var request=new TranslationPort.Request(NoticeLang.KO,language,"기숙사 신청 마감","기숙사 신청은 금요일 18시에 마감됩니다. 신청서는 국제처에 제출하세요.");
        var translated=llm.translate(request);
        var gate=new NoticeTranslationGate();
        var raw=gate.check(request,translated);
        // Explicit derived variant exercises the reviewed notation even if the model chooses 18:00.
        String local=translated.body().replaceFirst("18(?::00|\\s+Uhr|\\s*h(?:\\s*00)?)",
            language==NoticeLang.DE?"18:00 am Freitag":"18 h");
        var localVerdict=gate.check(request,new TranslationPort.Result(translated.title(),local,translated.modelId()));
        String corrupted=local.replace("18","19");
        var changed=gate.check(request,new TranslationPort.Result(translated.title(),corrupted,translated.modelId()));
        results.add(Map.of("language",language.code(),"model",translated.modelId(),"rawBody",translated.body(),
            "rawPassed",raw.passed(),"localizedVariant",local,"localizedPassed",localVerdict.passed(),"changedTimeRejected",!changed.passed()));
        assertThat(translated.modelId()).contains("opus");
        assertThat(raw.passed()).as("%s",raw.reasons()).isTrue();
        assertThat(local).contains(language==NoticeLang.DE?"18:00 am Freitag":"18 h");
        assertThat(localVerdict.passed()).as("%s",localVerdict.reasons()).isTrue();
        assertThat(corrupted).isNotEqualTo(local);
        assertThat(changed.passed()).isFalse();
    }
    @AfterAll static void evidence() throws Exception {
        var path=Path.of("build/reports/live-office-locales/results.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path,tools.jackson.databind.json.JsonMapper.builder().build().writerWithDefaultPrettyPrinter().writeValueAsString(results));
        assertThat(results).hasSize(2);
    }
}
