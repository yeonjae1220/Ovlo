package me.yeonjae.ovlo.shared.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 국제처 파일럿 메트릭(설계 §8·S6). 이름은 Prometheus 로 나가면 `ovlo_office_*` 가 된다.
 * 태그 값은 enum·고정 문자열만 — 질문 본문·회원 id 같은 고카디널리티 값은 절대 넣지 않는다.
 *
 * <pre>
 * ovlo.office.inquiry            counter  outcome=ANSWERED|NO_GROUNDS|ESCALATED
 * ovlo.office.inquiry.latency    timer    outcome
 * ovlo.office.llm.calls          counter  purpose=answer|translate  result=ok|refused|error  model
 * ovlo.office.llm.tokens         counter  purpose  direction=input|output  model
 * ovlo.office.llm.latency        timer    purpose  model
 * ovlo.office.consultation       counter  action=opened|assigned|resolved|closed
 * ovlo.office.partner.click      counter  category=SIM|INSURANCE|HOUSING|PICKUP
 * ovlo.office.notification       counter  kind=staff_invitation|notice_expiring|consultation_opened|notice_translation_failed  result=ok|error
 * </pre>
 */
@Component
public class OfficeMetrics {

    private final MeterRegistry registry;

    public OfficeMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void inquiry(InquiryOutcome outcome, long latencyMs) {
        String tag = outcome.name();
        Counter.builder("ovlo.office.inquiry").tag("outcome", tag).register(registry).increment();
        Timer.builder("ovlo.office.inquiry.latency").tag("outcome", tag).register(registry).record(Duration.ofMillis(latencyMs));
    }

    public void llmCall(String purpose, String model, String result, long inputTokens, long outputTokens, long ms) {
        String m = model == null || model.isBlank() ? "unknown" : model;
        Counter.builder("ovlo.office.llm.calls").tag("purpose", purpose).tag("result", result).tag("model", m)
                .register(registry).increment();
        if (inputTokens > 0) {
            Counter.builder("ovlo.office.llm.tokens").tag("purpose", purpose).tag("direction", "input").tag("model", m)
                    .register(registry).increment(inputTokens);
        }
        if (outputTokens > 0) {
            Counter.builder("ovlo.office.llm.tokens").tag("purpose", purpose).tag("direction", "output").tag("model", m)
                    .register(registry).increment(outputTokens);
        }
        Timer.builder("ovlo.office.llm.latency").tag("purpose", purpose).tag("model", m)
                .register(registry).record(Duration.ofMillis(ms));
    }

    public void consultation(String action) {
        Counter.builder("ovlo.office.consultation").tag("action", action).register(registry).increment();
    }

    public void partnerClick(PartnerCategory category) {
        Counter.builder("ovlo.office.partner.click").tag("category", category.name()).register(registry).increment();
    }

    /** 알림 메일 결과. error 가 늘면 SMTP 자격증명부터 본다(런북 §7). */
    public void notification(String kind, String result) {
        Counter.builder("ovlo.office.notification").tag("kind", kind).tag("result", result).register(registry).increment();
    }
}
