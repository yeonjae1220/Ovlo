package me.yeonjae.ovlo.shared.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import me.yeonjae.ovlo.domain.office.model.InquiryRejectionReason;
import me.yeonjae.ovlo.application.service.office.LongNoticeSavedEvent;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 국제처 파일럿 메트릭(설계 §8·S6). 이름은 Prometheus 로 나가면 `ovlo_office_*` 가 된다.
 * 태그 값은 enum·고정 문자열만 — 질문 본문·회원 id 같은 고카디널리티 값은 절대 넣지 않는다.
 *
 * <pre>
 * ovlo.office.search.completed  counter  provider=KEYWORD|POSTGRES  path=NO_ELIGIBLE|SMALL_CORPUS|RANKED  candidates=EMPTY|PRESENT
 * ovlo.office.inquiry.retrieval counter  query_kind=SINGLE|FOLLOW_UP  result=EMPTY|PRESENT|ERROR
 * ovlo.office.notice.long_body   counter  action=REGISTER|UPDATE (committed long-body saves, not unique notices)
 * ovlo.office.inquiry            counter  outcome=ANSWERED|NO_GROUNDS|OUT_OF_SCOPE|ESCALATED  rejection_reason=NONE|enum  (exported; zero series at startup)
 * ovlo.office.inquiry.latency    timer    outcome
 * ovlo.office.llm.calls          counter  purpose=answer|inquiry-screen|inquiry-verify|translate|marketplace-translate  result=ok|refused|error  model
 * ovlo.office.llm.tokens         counter  purpose  direction=input|output  model
 * ovlo.office.llm.latency        timer    purpose  model
 * ovlo.office.consultation       counter  action=opened|assigned|resolved|closed
 * ovlo.office.partner.click      counter  category=SIM|INSURANCE|HOUSING|PICKUP
 * ovlo.office.notification       counter  kind=staff_invitation|notice_expiring|consultation_opened|notice_translation_failed  result=ok|error
 * </pre>
 */
@Component
public class OfficeMetrics {

    public enum SearchProvider { KEYWORD, POSTGRES }
    public enum SearchPath { NO_ELIGIBLE, SMALL_CORPUS, RANKED }
    public enum RetrievalResult { EMPTY, PRESENT, ERROR }

    /** Tag value for an inquiry saved without a rejection reason. */
    public static final String NO_REJECTION_REASON = "NONE";

    private final MeterRegistry registry;

    public OfficeMetrics(MeterRegistry registry) {
        this.registry = registry;
        // Fixed enum/boolean dimensions only. Expose zero series before the first real search.
        for (var provider : SearchProvider.values()) {
            for (var path : SearchPath.values()) {
                for (boolean present : new boolean[]{false, true}) searchCounter(provider, path, present);
            }
        }
        for (boolean followUp : new boolean[]{false, true}) {
            for (var result : RetrievalResult.values()) retrievalCounter(followUp, result);
        }
        for (var action : LongNoticeSavedEvent.Action.values()) {
            longNoticeCounter(action);
        }
        // Zero series for every outcome, and for each reason an unanswered outcome can carry, so the first
        // occurrence after a restart is a visible 0 -> 1 step rather than a series that appears at 1.
        for (var outcome : InquiryOutcome.values()) {
            inquiryCounter(outcome, null);
            if (outcome == InquiryOutcome.NO_GROUNDS || outcome == InquiryOutcome.ESCALATED) {
                for (var reason : InquiryRejectionReason.values()) inquiryCounter(outcome, reason);
            }
        }
    }

    /** One successful outer adapter call, before the service rechecks eligibility. */
    public void searchCompleted(SearchProvider provider, SearchPath path, boolean hasCandidates) {
        searchCounter(provider, path, hasCandidates).increment();
    }

    private Counter searchCounter(SearchProvider provider, SearchPath path, boolean hasCandidates) {
        return Counter.builder("ovlo.office.search.completed")
                .description("Completed outer notice searches; internal CJK fallback calls are excluded")
                .tag("provider", provider.name()).tag("path", path.name())
                .tag("candidates", hasCandidates ? "PRESENT" : "EMPTY").register(registry);
    }

    /** One search-stage observation after admission/screening, not a saved inquiry or answer outcome. */
    public void inquiryRetrieval(boolean followUp, RetrievalResult result) {
        retrievalCounter(followUp, result).increment();
    }

    private Counter retrievalCounter(boolean followUp, RetrievalResult result) {
        return Counter.builder("ovlo.office.inquiry.retrieval")
                .description("Inquiry retrieval stage after final eligibility filtering, including stage errors")
                .tag("query_kind", followUp ? "FOLLOW_UP" : "SINGLE")
                .tag("result", result.name()).register(registry);
    }

    public void longNoticeSaved(LongNoticeSavedEvent.Action action) {
        longNoticeCounter(action).increment();
    }

    private Counter longNoticeCounter(LongNoticeSavedEvent.Action action) {
        return Counter.builder("ovlo.office.notice.long_body")
                .description("Committed notice saves whose original body exceeds the answer context character limit")
                .tag("action", action.name()).register(registry);
    }

    public void inquiry(InquiryOutcome outcome, long latencyMs) {
        inquiry(outcome, latencyMs, null);
    }

    public void inquiry(InquiryOutcome outcome, long latencyMs, InquiryRejectionReason reason) {
        String tag = outcome.name();
        inquiryCounter(outcome, reason).increment();
        Timer.builder("ovlo.office.inquiry.latency").tag("outcome", tag).register(registry).record(Duration.ofMillis(latencyMs));
    }

    private Counter inquiryCounter(InquiryOutcome outcome, InquiryRejectionReason reason) {
        return Counter.builder("ovlo.office.inquiry")
                .description("Saved inquiry outcomes; rejection_reason is NONE unless the inquiry went unanswered")
                .tag("outcome", outcome.name())
                .tag("rejection_reason", reason == null ? NO_REJECTION_REASON : reason.name()).register(registry);
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
