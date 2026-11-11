package me.yeonjae.ovlo.application.port.out.marketplace;

import me.yeonjae.ovlo.domain.marketplace.model.*;
import me.yeonjae.ovlo.domain.media.model.StoragePath;
import me.yeonjae.ovlo.application.dto.command.MarketplaceSearch;
import me.yeonjae.ovlo.application.dto.result.ListingTranslation;
import java.util.*;

public interface MarketplaceStore {
    Optional<Listing> find(long id, boolean lock);
    List<Listing> search(long actor, MarketplaceSearch search);
    Listing save(Listing listing, long expectedVersion);
    record Photo(UUID id, long owner, StoragePath path) {}
    void addPhoto(Photo photo);
    Optional<Photo> photo(UUID id);
    boolean photoVisible(UUID id, long viewer);
    void validatePhotos(long owner, Long listingId, List<UUID> ids);
    boolean blocked(long a, long b);
    void block(long actor, long target, boolean blocked);
    Optional<Long> chat(long listingId, long buyerId);
    void linkChat(long listingId, long buyerId, long roomId);
    void report(long listingId, long reporter, String reason);
    record Report(long id, long listingId, String title, String reason, boolean hidden, long version) {}
    List<Report> reports();
    void audit(long listingId, String actor, String action);
    Optional<ListingTranslation> translation(long listingId, long version, String language);
    void cacheTranslation(ListingTranslation translation);
}
