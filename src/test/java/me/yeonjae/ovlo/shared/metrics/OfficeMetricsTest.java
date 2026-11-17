package me.yeonjae.ovlo.shared.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OfficeMetricsTest {

    @Test void searchMetricsExposeOnlyFixedDimensionsAndStartAtZero() {
        var registry = new io.micrometer.prometheusmetrics.PrometheusMeterRegistry(
                io.micrometer.prometheusmetrics.PrometheusConfig.DEFAULT);
        try {
            var metrics = new OfficeMetrics(registry);
            var searches = registry.find("ovlo.office.search.completed").counters();
            var retrievals = registry.find("ovlo.office.inquiry.retrieval").counters();
            assertThat(searches).hasSize(12).allSatisfy(c -> {
                assertThat(c.count()).isZero();
                assertThat(c.getId().getTags()).extracting(t -> t.getKey())
                        .containsExactlyInAnyOrder("provider", "path", "candidates");
            });
            assertThat(retrievals).hasSize(6).allSatisfy(c -> {
                assertThat(c.count()).isZero();
                assertThat(c.getId().getTags()).extracting(t -> t.getKey())
                        .containsExactlyInAnyOrder("query_kind", "result");
            });
            for (var provider : OfficeMetrics.SearchProvider.values()) {
                for (var path : OfficeMetrics.SearchPath.values()) {
                    for (boolean present : new boolean[]{false, true}) metrics.searchCompleted(provider, path, present);
                }
            }
            for (boolean followUp : new boolean[]{false, true}) {
                for (var result : OfficeMetrics.RetrievalResult.values()) metrics.inquiryRetrieval(followUp, result);
            }
            assertThat(registry.find("ovlo.office.search.completed").counters()).hasSize(12)
                    .allSatisfy(c -> assertThat(c.count()).isEqualTo(1));
            assertThat(registry.find("ovlo.office.inquiry.retrieval").counters()).hasSize(6)
                    .allSatisfy(c -> assertThat(c.count()).isEqualTo(1));
            assertThat(registry.scrape()).contains("ovlo_office_search_completed_total{", "ovlo_office_inquiry_retrieval_total{");
        } finally {
            registry.close();
        }
    }

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
    @Test void allOutcomesUseTheSameBoundedTagKeys() {
        var registry = new SimpleMeterRegistry();
        var metrics = new OfficeMetrics(registry);
        metrics.inquiry(InquiryOutcome.ANSWERED, 1, null);
        metrics.inquiry(InquiryOutcome.OUT_OF_SCOPE, 1, null);
        for (var reason : me.yeonjae.ovlo.domain.office.model.InquiryRejectionReason.values()) {
            metrics.inquiry(InquiryOutcome.NO_GROUNDS, 1, reason);
            assertThat(registry.get("ovlo.office.inquiry").tag("rejection_reason", reason.name()).counter().count()).isEqualTo(1);
        }
        assertThat(registry.get("ovlo.office.inquiry").tag("outcome", "ANSWERED").tag("rejection_reason", "NONE").counter().count()).isEqualTo(1);
    }

}
