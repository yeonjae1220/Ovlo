package me.yeonjae.ovlo.adapter.in.scheduler;

import me.yeonjae.ovlo.application.port.in.marketplace.CleanupMarketplacePhotosUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="ovlo.marketplace.photos.cleanup-enabled",havingValue="true",matchIfMissing=true)
public class MarketplacePhotoCleanupScheduler {
    private final CleanupMarketplacePhotosUseCase cleanup;
    public MarketplacePhotoCleanupScheduler(CleanupMarketplacePhotosUseCase cleanup) { this.cleanup=cleanup; }
    @Scheduled(cron="${ovlo.marketplace.photos.cleanup-cron:0 40 * * * *}")
    public void run() { cleanup.cleanup(); }
}
