package me.yeonjae.ovlo.application.service.command;

import java.time.Duration;
import java.time.Instant;
import me.yeonjae.ovlo.application.port.in.marketplace.CleanupMarketplacePhotosUseCase;
import me.yeonjae.ovlo.application.port.out.marketplace.UnusedMarketplacePhotoStore;
import me.yeonjae.ovlo.application.port.out.media.StoragePort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class MarketplacePhotoCleanupService implements CleanupMarketplacePhotosUseCase {
    private static final Logger log=LoggerFactory.getLogger(MarketplacePhotoCleanupService.class);
    private final UnusedMarketplacePhotoStore photos;
    private final StoragePort storage;
    private final TransactionTemplate transaction;
    private final Duration retention;
    private final int batchSize;
    private final Counter removedCounter,failedCounter;
    public MarketplacePhotoCleanupService(UnusedMarketplacePhotoStore photos,StoragePort storage,
            PlatformTransactionManager tx,MeterRegistry metrics,
            @Value("${ovlo.marketplace.photos.retention-hours:72}") long hours,
            @Value("${ovlo.marketplace.photos.cleanup-batch-size:100}") int batchSize) {
        if(hours<1 || batchSize<1 || batchSize>1000) throw new IllegalArgumentException("Invalid photo cleanup limits");
        this.photos=photos; this.storage=storage; this.transaction=new TransactionTemplate(tx);
        this.retention=Duration.ofHours(hours); this.batchSize=batchSize;
        this.removedCounter=metrics.counter("ovlo.marketplace.photo.cleanup.removed");
        this.failedCounter=metrics.counter("ovlo.marketplace.photo.cleanup.failed");
    }
    @Override public Result cleanup() {
        Instant now=Instant.now();
        var batch=transaction.execute(tx->photos.claim(now.minus(retention),now.plus(Duration.ofHours(1)),batchSize));
        int removed=0,failed=0;
        for(var photo:batch) {
            try {
                // Durable pending state blocks reads/attachments. No DB lock is held during storage I/O.
                // A crash after delete is safe: StoragePort.delete must be idempotent on missing files.
                storage.delete(photo.path());
                transaction.executeWithoutResult(tx->photos.complete(photo.id()));
                removed++; removedCounter.increment();
            } catch(RuntimeException e) {
                failed++; failedCounter.increment();
                log.warn("Marketplace photo cleanup will retry photo={} kind={}",photo.id(),e.getClass().getSimpleName());
            }
        }
        return new Result(batch.size(),removed,failed);
    }
}
