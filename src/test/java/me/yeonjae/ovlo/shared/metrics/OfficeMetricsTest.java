package me.yeonjae.ovlo.shared.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OfficeMetricsTest {

    @Test
    @DisplayName("문의·LLM·상담·클릭이 고정 태그로 집계된다 (토큰은 방향별 누적, 0 이면 생략)")
    void recordsWithFixedTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OfficeMetrics m = new OfficeMetrics(registry);

        m.inquiry(InquiryOutcome.ANSWERED, 800);
        m.inquiry(InquiryOutcome.ANSWERED, 400);
        m.inquiry(InquiryOutcome.NO_GROUNDS, 100);
        m.llmCall("answer", "claude-opus-5-5", "ok", 1200, 300, 2500);
        m.llmCall("translate", null, "error", 0, 0, 90);
        m.consultation("opened");
        m.partnerClick(PartnerCategory.SIM);
        m.notification("staff_invitation", "error");

        assertThat(registry.get("ovlo.office.inquiry").tag("outcome", "ANSWERED").counter().count()).isEqualTo(2);
        assertThat(registry.get("ovlo.office.inquiry.latency").tag("outcome", "ANSWERED").timer().count()).isEqualTo(2);
        assertThat(registry.get("ovlo.office.llm.tokens").tag("direction", "input").tag("model", "claude-opus-5-5").counter().count()).isEqualTo(1200);
        assertThat(registry.get("ovlo.office.llm.calls").tag("result", "error").tag("model", "unknown").counter().count()).isEqualTo(1);
        assertThat(registry.find("ovlo.office.llm.tokens").tag("purpose", "translate").counter()).isNull();
        assertThat(registry.get("ovlo.office.consultation").tag("action", "opened").counter().count()).isEqualTo(1);
        assertThat(registry.get("ovlo.office.partner.click").tag("category", "SIM").counter().count()).isEqualTo(1);
        assertThat(registry.get("ovlo.office.notification").tag("kind", "staff_invitation").tag("result", "error").counter().count()).isEqualTo(1);
    }
}
