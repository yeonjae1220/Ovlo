package me.yeonjae.ovlo.shared.config;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.registry.otlp.AggregationTemporality;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

/** Only the two anonymous retrieval counters are pushed to the internal collector. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "ovlo.search.metrics.export.enabled", havingValue = "true")
public class SearchMetricsExportConfig {
    private static final Map<String, Set<String>> SEARCH_TAGS = Map.of(
            "provider", Set.of("KEYWORD", "POSTGRES"),
            "path", Set.of("NO_ELIGIBLE", "SMALL_CORPUS", "RANKED"),
            "candidates", Set.of("EMPTY", "PRESENT"));
    private static final Map<String, Set<String>> RETRIEVAL_TAGS = Map.of(
            "query_kind", Set.of("SINGLE", "FOLLOW_UP"), "result", Set.of("EMPTY", "PRESENT", "ERROR"));

    @Bean(destroyMethod = "close")
    OtlpMeterRegistry searchMetricsRegistry(@Value("${ovlo.search.metrics.export.url}") String url,
            @Value("${HOSTNAME:}") String instance,
            @Value("${ovlo.search.metrics.export.step:30s}") Duration step) {
        String instanceId = instance.isBlank() ? java.util.UUID.randomUUID().toString() : instance;
        OtlpConfig config = new OtlpConfig() {
            public String get(String key) { return null; }
            public String url() { return url; }
            public Duration step() { return step; }
            public Map<String, String> headers() { return Map.of(); }
            public AggregationTemporality aggregationTemporality() { return AggregationTemporality.CUMULATIVE; }
            public Map<String, String> resourceAttributes() {
                return Map.of("service.name", "ovlo-search-metrics", "service.namespace", "ovlo", "service.instance.id", instanceId);
            }
        };
        // This constructor has no exemplar context provider: no trace/span correlation is exported.
        OtlpMeterRegistry registry = new OtlpMeterRegistry(config, Clock.SYSTEM);
        registry.config().meterFilter(new MeterFilter() {
            public MeterFilterReply accept(Meter.Id id) {
                return allowed(id) ? MeterFilterReply.ACCEPT : MeterFilterReply.DENY;
            }
        });
        return registry;
    }

    static boolean allowed(Meter.Id id) {
        if (id.getType() != Meter.Type.COUNTER) return false;
        Map<String, Set<String>> tags = switch (id.getName()) {
            case "ovlo.office.search.completed" -> SEARCH_TAGS;
            case "ovlo.office.inquiry.retrieval" -> RETRIEVAL_TAGS;
            default -> Map.of();
        };
        return !tags.isEmpty() && id.getTags().size() == tags.size()
                && id.getTags().stream().allMatch(t -> tags.getOrDefault(t.getKey(), Set.of()).contains(t.getValue()));
    }
}
