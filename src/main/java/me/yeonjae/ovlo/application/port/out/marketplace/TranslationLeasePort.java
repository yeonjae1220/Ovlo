package me.yeonjae.ovlo.application.port.out.marketplace;

import java.time.Duration;
import java.util.UUID;

/** Cross-instance ownership. Only the current, unexpired owner may publish. */
public interface TranslationLeasePort {
    record Key(long listingId, long version, String language) {}
    boolean acquire(Key key, UUID owner, Duration lifetime);
    boolean renew(Key key, UUID owner, Duration lifetime);
    /** Inside the short cache-write transaction; locks ownership until commit. */
    boolean lockOwned(Key key, UUID owner);
    void release(Key key, UUID owner);
}
