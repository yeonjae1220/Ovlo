package me.yeonjae.ovlo.adapter.out.office;

import me.yeonjae.ovlo.application.port.out.office.TranslationPort;
import me.yeonjae.ovlo.application.service.office.NoticeTranslationGate;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.*;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;

/** Korean section markers are identifiers, not letters to translate to A/B or an invented ordinal. */
@Tag("live-llm") @EnabledIfEnvironmentVariable(named="OVLO_LIVE_LLM",matches="1")
class NoticeTranslationLabelLiveEvaluationTest {
    static final List<Map<String,Object>> RESULTS=new ArrayList<>();
    @ParameterizedTest @EnumSource(value=NoticeLang.class,names={"EN","DE","FR","JA","ZH","VI"})
    void preservesSectionLabelAndPassesIndependentVerification(NoticeLang lang) {
        var llm=new AnthropicLlmAdapter(new me.yeonjae.ovlo.shared.metrics.OfficeMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()),
                System.getenv("ANTHROPIC_API_KEY"),System.getenv("OVLO_LLM_BASE_URL"),"claude-opus-5-5","medium",2048,90,false);
        var request=new TranslationPort.Request(NoticeLang.KO,lang,"[운영 검증 전용] 등록 안내 나",
                "운영 검증용 가상 공지이며 실제 학교 안내가 아닙니다. 등록비는 15유로입니다. 보증금은 150유로입니다. 등록비는 환불되지 않습니다. 안내 교육은 1.5시간입니다. 문의 office@university.example https://university.example/help");
        var result=llm.translate(request);var gate=new NoticeTranslationGate().check(request,result);
        boolean semantic=llm.verifyTranslation(request,result);
        RESULTS.add(Map.of("language",lang.code(),"title",result.title(),"body",result.body(),"gate",gate,"semantic",semantic));
        assertThat(result.title()).contains("나");
        assertThat(gate.passed()).as(gate.toString()).isTrue();assertThat(semantic).as(lang.toString()).isTrue();
    }
    @AfterAll static void evidence() throws Exception {
        var path=Path.of("build/reports/live-notice-labels/results.json");Files.createDirectories(path.getParent());
        Files.writeString(path,tools.jackson.databind.json.JsonMapper.builder().build().writerWithDefaultPrettyPrinter().writeValueAsString(RESULTS));
        assertThat(RESULTS).hasSize(6);
    }
}
