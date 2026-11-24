package me.yeonjae.ovlo.shared.config;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import me.yeonjae.ovlo.shared.metrics.OfficeMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

class SearchMetricsExportConfigTest {
    @Test void disabledUnlessExplicitlyEnabled() {
        new ApplicationContextRunner().withUserConfiguration(SearchMetricsExportConfig.class)
                .run(c -> assertThat(c).doesNotHaveBean(io.micrometer.registry.otlp.OtlpMeterRegistry.class));
    }

    @Test void actualOtlpRequestContainsOnlyAllowedCountersWithoutExemplars() throws Exception {
        var bodies = new LinkedBlockingQueue<byte[]>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/metrics", exchange -> {
            bodies.add(exchange.getRequestBody().readAllBytes());
            exchange.sendResponseHeaders(200, -1); exchange.close();
        });
        server.start();
        var registry = new SearchMetricsExportConfig().searchMetricsRegistry(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/metrics", "synthetic-pod", Duration.ofMillis(200));
        try {
            var metrics = new OfficeMetrics(registry);
            metrics.searchCompleted(OfficeMetrics.SearchProvider.POSTGRES, OfficeMetrics.SearchPath.RANKED, true);
            metrics.inquiryRetrieval(true, OfficeMetrics.RetrievalResult.PRESENT);
            metrics.inquiry(me.yeonjae.ovlo.domain.office.model.InquiryOutcome.NO_GROUNDS, 120,
                    me.yeonjae.ovlo.domain.office.model.InquiryRejectionReason.NO_ELIGIBLE_NOTICES);
            metrics.llmCall("answer", "claude-sonnet-5-5", "ok", 100, 20, 250);
            metrics.llmCall("answer", "PRIVATE_SENTINEL", "ok", 100, 20, 250);
            registry.counter("ovlo.office.llm.tokens", "purpose", "answer", "model", "claude-sonnet-5-5", "direction", "input", "member_id", "PRIVATE_SENTINEL").increment();
            registry.counter("ovlo.office.inquiry", "outcome", "NO_GROUNDS", "rejection_reason", "PRIVATE_SENTINEL").increment();
            registry.counter("ovlo.office.inquiry", "outcome", "ANSWERED", "rejection_reason", "NONE",
                    "office_id", "PRIVATE_SENTINEL").increment();
            registry.counter("forbidden.counter", "email", "PRIVATE_SENTINEL").increment();
            registry.counter("ovlo.office.search.completed", "provider", "POSTGRES", "path", "RANKED",
                    "candidates", "PRESENT", "member_id", "PRIVATE_SENTINEL").increment();
            registry.counter("ovlo.office.inquiry.retrieval", "query_kind", "PRIVATE_SENTINEL", "result", "PRESENT").increment();
            assertThat(registry.getMeters()).hasSize(43);
            String body = null;
            ExportMetricsServiceRequest payload = null;
            for (int i=0; i<20; i++) {
                byte[] candidate = bodies.poll(1, TimeUnit.SECONDS);
                if (candidate != null && new String(candidate, StandardCharsets.UTF_8).contains("ovlo.office.inquiry.retrieval")) {
                    body = new String(candidate, StandardCharsets.UTF_8);
                    payload = ExportMetricsServiceRequest.parseFrom(candidate);
                    break;
                }
            }
            assertThat(body).isNotNull().contains("ovlo.office.search.completed", "ovlo.office.inquiry.retrieval", "synthetic-pod",
                            "NO_ELIGIBLE_NOTICES")
                    .contains("ovlo.office.llm.tokens", "ovlo.office.inquiry.latency")
                    .doesNotContain("forbidden.counter", "PRIVATE_SENTINEL", "long_body", "traceId", "spanId", "trace_id", "span_id");
            assertThat(payload).isNotNull();
            var exported = payload.getResourceMetricsList().stream()
                    .flatMap(r -> r.getScopeMetricsList().stream()).flatMap(s -> s.getMetricsList().stream()).toList();
            assertThat(exported).isNotEmpty().allSatisfy(metric -> {
                assertThat(metric.getName()).isIn("ovlo.office.search.completed", "ovlo.office.inquiry.retrieval",
                        "ovlo.office.inquiry", "ovlo.office.inquiry.latency", "ovlo.office.llm.calls", "ovlo.office.llm.tokens", "ovlo.office.llm.latency");
                if (metric.hasSum()) {
                    assertThat(metric.getSum().getIsMonotonic()).isTrue();
                    assertThat(metric.getSum().getDataPointsList()).isNotEmpty().allSatisfy(point -> assertThat(point.getExemplarsList()).isEmpty());
                } else {
                    assertThat(metric.hasHistogram()).isTrue();
                    assertThat(metric.getHistogram().getDataPointsList()).isNotEmpty().allSatisfy(point -> assertThat(point.getExemplarsList()).isEmpty());
                }
            });
        } finally { registry.close(); server.stop(0); }
    }

    @Test void productionConfigCreatesOneFilteredExporterAndPreservesPrometheus() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/metrics", e -> { e.getRequestBody().readAllBytes(); e.sendResponseHeaders(200, -1); e.close(); });
        server.start();
        try {
        new ApplicationContextRunner()
                .withInitializer(c -> c.getBeanFactory().setConversionService(new org.springframework.boot.convert.ApplicationConversionService()))
                .withInitializer(new org.springframework.boot.test.context.ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=prod", "ovlo.search.metrics.export.step=1h",
                        "ovlo.search.metrics.export.url=http://127.0.0.1:" + server.getAddress().getPort() + "/v1/metrics")
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration.class,
                        org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration.class,
                        org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsExportAutoConfiguration.class,
                        org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.PrometheusMetricsExportAutoConfiguration.class))
                .withUserConfiguration(SearchMetricsExportConfig.class)
                .run(c -> {
                    assertThat(c).hasSingleBean(io.micrometer.registry.otlp.OtlpMeterRegistry.class)
                            .hasSingleBean(io.micrometer.prometheusmetrics.PrometheusMeterRegistry.class);
                    assertThat(c.getEnvironment().getProperty("management.otlp.metrics.export.enabled")).isEqualTo("false");
                    var composite = c.getBean(io.micrometer.core.instrument.MeterRegistry.class);
                    new OfficeMetrics(composite).inquiryRetrieval(false, OfficeMetrics.RetrievalResult.EMPTY);
                    composite.counter("forbidden.counter", "email", "PRIVATE_SENTINEL").increment();
                    assertThat(c.getBean(io.micrometer.registry.otlp.OtlpMeterRegistry.class).getMeters()).hasSize(38);
                    assertThat(c.getBean(io.micrometer.prometheusmetrics.PrometheusMeterRegistry.class)
                            .find("forbidden.counter").counter()).isNotNull();
                    // The real network exporter is covered separately; this test checks registry wiring.
                    c.getBean(io.micrometer.registry.otlp.OtlpMeterRegistry.class).clear();
                });
        } finally { server.stop(0); }
    }

    @Test void unexpectedMeterTypesAndTagValuesFailClosed() {
        assertThat(SearchMetricsExportConfig.allowed(new Meter.Id("ovlo.office.inquiry.retrieval",
                Tags.of("query_kind", "SINGLE", "result", "PRESENT"), null, null, Meter.Type.GAUGE))).isFalse();
        assertThat(SearchMetricsExportConfig.allowed(new Meter.Id("ovlo.office.inquiry.retrieval",
                Tags.of("query_kind", "SINGLE", "result", "QUESTION_TEXT"), null, null, Meter.Type.COUNTER))).isFalse();
    }

    @Test void savedOutcomeCounterIsExportedOnlyWithKnownOutcomeAndReasonValues() {
        for (var outcome : me.yeonjae.ovlo.domain.office.model.InquiryOutcome.values()) {
            assertThat(SearchMetricsExportConfig.allowed(inquiryId(Meter.Type.COUNTER, "outcome", outcome.name(),
                    "rejection_reason", "NONE"))).isTrue();
        }
        for (var reason : me.yeonjae.ovlo.domain.office.model.InquiryRejectionReason.values()) {
            assertThat(SearchMetricsExportConfig.allowed(inquiryId(Meter.Type.COUNTER, "outcome", "NO_GROUNDS",
                    "rejection_reason", reason.name()))).isTrue();
        }
        assertThat(SearchMetricsExportConfig.allowed(inquiryId(Meter.Type.COUNTER, "outcome", "NO_GROUNDS",
                "rejection_reason", "FREE_TEXT"))).isFalse();
        assertThat(SearchMetricsExportConfig.allowed(inquiryId(Meter.Type.COUNTER, "outcome", "ANSWERED"))).isFalse();
        assertThat(SearchMetricsExportConfig.allowed(inquiryId(Meter.Type.COUNTER, "outcome", "ANSWERED",
                "rejection_reason", "NONE", "office_id", "2"))).isFalse();
        assertThat(SearchMetricsExportConfig.allowed(inquiryId(Meter.Type.TIMER, "outcome", "ANSWERED",
                "rejection_reason", "NONE"))).isFalse();
        assertThat(SearchMetricsExportConfig.allowed(new Meter.Id("ovlo.office.inquiry.latency",
                Tags.of("outcome", "ANSWERED"), null, null, Meter.Type.TIMER))).isTrue();
        assertThat(SearchMetricsExportConfig.allowed(new Meter.Id("ovlo.office.inquiry.latency",
                Tags.of("outcome", "ANSWERED", "office_id", "private"), null, null, Meter.Type.TIMER))).isFalse();
    }

    private static Meter.Id inquiryId(Meter.Type type, String... tags) {
        return new Meter.Id("ovlo.office.inquiry", Tags.of(tags), null, null, type);
    }
}
