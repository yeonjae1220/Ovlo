package me.yeonjae.ovlo.shared.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfficePilotConfigGuardTest {

    @Test
    @DisplayName("anthropic + 키 + postgres 면 통과")
    void passesWhenProductionReady() {
        assertThat(OfficePilotConfigGuard.check("anthropic", "sk-test", "postgres")).isEmpty();
        assertThatCode(() -> new OfficePilotConfigGuard("anthropic", "sk-test", "postgres", false)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("fake LLM·빈 키·keyword 검색은 각각 문제로 잡히고 기동이 막힌다")
    void failsOnDegradedSettings() {
        assertThat(OfficePilotConfigGuard.check("fake", "", "keyword")).hasSize(2);
        assertThat(OfficePilotConfigGuard.check("anthropic", " ", "postgres")).hasSize(1).first().asString().contains("ANTHROPIC_API_KEY");
        assertThatThrownBy(() -> new OfficePilotConfigGuard("fake", "", "keyword", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OVLO_LLM_PROVIDER=fake")
                .hasMessageContaining("OVLO_SEARCH_PROVIDER=keyword");
    }

    @Test
    @DisplayName("allow-degraded 면 경고만 남기고 뜬다")
    void allowDegradedOnlyWarns() {
        assertThatCode(() -> new OfficePilotConfigGuard("fake", "", "keyword", true)).doesNotThrowAnyException();
    }
}
