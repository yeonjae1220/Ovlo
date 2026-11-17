package me.yeonjae.ovlo.application.service.office;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import me.yeonjae.ovlo.shared.metrics.OfficeMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class LongNoticeMetricsListenerTest {
    @Test
    void countsOnlyCommittedEventsWithBoundedTags() {
        var registry = new SimpleMeterRegistry();
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(TransactionalEventListenerFactory.class);
            context.registerBean(OfficeMetrics.class, () -> new OfficeMetrics(registry));
            context.register(LongNoticeMetricsListener.class);
            context.refresh();
            var transaction = new TransactionTemplate(new AbstractPlatformTransactionManager() {
                protected Object doGetTransaction() { return new Object(); }
                protected void doBegin(Object tx, TransactionDefinition definition) { }
                protected void doCommit(DefaultTransactionStatus status) { }
                protected void doRollback(DefaultTransactionStatus status) { }
            });
            for (var action : LongNoticeSavedEvent.Action.values()) {
                var counter = registry.get("ovlo.office.notice.long_body").tag("action", action.name()).counter();
                assertThat(counter.count()).isZero();
                transaction.executeWithoutResult(status -> {
                    context.publishEvent(new LongNoticeSavedEvent(action));
                    assertThat(counter.count()).isZero();
                    status.setRollbackOnly();
                });
                assertThat(counter.count()).isZero();
                transaction.executeWithoutResult(status -> {
                    context.publishEvent(new LongNoticeSavedEvent(action));
                    assertThat(counter.count()).isZero();
                });
                assertThat(counter.count()).isEqualTo(1);
                context.publishEvent(new LongNoticeSavedEvent(action));
                assertThat(counter.count()).isEqualTo(1);
                assertThat(counter.getId().getTags()).extracting(t -> t.getKey()).containsExactly("action");
            }
        }
    }
}
