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
import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import me.yeonjae.ovlo.domain.office.model.InquiryRejectionReason;
import me.yeonjae.ovlo.shared.metrics.OfficeMetrics;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Only the anonymous retrieval counters and the saved-outcome counter are pushed to the internal collector. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "ovlo.search.metrics.export.enabled", havingValue = "true")
public class SearchMetricsExportConfig {
    private static final Map<String, Set<String>> SEARCH_TAGS = Map.of(
            "provider", Set.of("KEYWORD", "POSTGRES"),
            "path", Set.of("NO_ELIGIBLE", "SMALL_CORPUS", "RANKED"),
            "candidates", Set.of("EMPTY", "PRESENT"));
    private static final Map<String, Set<String>> RETRIEVAL_TAGS = Map.of(
            "query_kind", Set.of("SINGLE", "FOLLOW_UP"), "result", Set.of("EMPTY", "PRESENT", "ERROR"));

    private static final Map<String, Set<String>> INQUIRY_TAGS = Map.of(
            "outcome", names(InquiryOutcome.values()),
            "rejection_reason", Stream.concat(Stream.of(OfficeMetrics.NO_REJECTION_REASON),
                    names(InquiryRejectionReason.values()).stream()).collect(Collectors.toUnmodifiableSet()));

    private static Set<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).collect(Collectors.toUnmodifiableSet());
    }

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
            case "ovlo.office.inquiry" -> INQUIRY_TAGS;
            default -> Map.of();
        };
        return !tags.isEmpty() && id.getTags().size() == tags.size()
                && id.getTags().stream().allMatch(t -> tags.getOrDefault(t.getKey(), Set.of()).contains(t.getValue()));
    }
}
