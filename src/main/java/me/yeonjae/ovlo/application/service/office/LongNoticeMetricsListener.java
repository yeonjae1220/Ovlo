package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.shared.metrics.OfficeMetrics;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class LongNoticeMetricsListener {
    private final OfficeMetrics metrics;

    public LongNoticeMetricsListener(OfficeMetrics metrics) {
        this.metrics = metrics;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSaved(LongNoticeSavedEvent event) {
        metrics.longNoticeSaved(event.action());
    }
}
