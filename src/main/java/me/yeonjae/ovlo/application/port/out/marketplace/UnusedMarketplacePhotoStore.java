package me.yeonjae.ovlo.application.port.out.marketplace;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UnusedMarketplacePhotoStore {
    /** Atomically rechecks attachment and marks candidates unavailable for future attachment. */
    List<MarketplaceStore.Photo> claim(Instant cutoff, Instant retryAfter, int limit);
    void complete(UUID photoId);
}
