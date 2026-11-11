package me.yeonjae.ovlo.domain.marketplace.model;

import java.time.Instant;
import java.util.Objects;
import me.yeonjae.ovlo.domain.marketplace.exception.MarketplaceException;
import static me.yeonjae.ovlo.domain.marketplace.exception.MarketplaceException.Kind.*;

public final class Listing {
    private final Long id;
    private final long sellerId;
    private ListingDetails details;
    private ListingStatus status;
    private boolean hidden;
    private boolean deleted;
    private final long version;
    private final Instant createdAt;
    private Listing(Long id, long sellerId, ListingDetails details, ListingStatus status, boolean hidden, boolean deleted, long version, Instant createdAt) {
        this.id=id; this.sellerId=sellerId; this.details=Objects.requireNonNull(details); this.status=status;
        this.hidden=hidden; this.deleted=deleted; this.version=version; this.createdAt=createdAt;
    }
    public static Listing create(long sellerId, ListingDetails details) {
        if (sellerId <= 0) throw new IllegalArgumentException("Seller required");
        return new Listing(null, sellerId, details, ListingStatus.AVAILABLE, false, false, 0, Instant.now());
    }
    public static Listing restore(long id, long sellerId, ListingDetails details, ListingStatus status, boolean hidden, boolean deleted, long version, Instant createdAt) {
        return new Listing(id, sellerId, details, status, hidden, deleted, version, createdAt);
    }
    public void requireOwner(long actor) { if (actor != sellerId) throw new MarketplaceException(FORBIDDEN, "Only the owner can change this listing"); }
    private void editable(long actor) {
        requireOwner(actor);
        if (hidden || deleted) throw new MarketplaceException(CONFLICT, "Listing is hidden or deleted");
        if (status == ListingStatus.COMPLETED) throw new MarketplaceException(CONFLICT, "Listing is completed");
    }
    public void edit(long actor, ListingDetails next) {
        editable(actor);
        if (status == ListingStatus.PARTIALLY_TAKEN && !next.partialPickup()) throw new MarketplaceException(CONFLICT, "Keep partial pickup enabled or change status first");
        details=Objects.requireNonNull(next);
    }
    public void changeStatus(long actor, ListingStatus next) {
        editable(actor); Objects.requireNonNull(next);
        if (next == ListingStatus.PARTIALLY_TAKEN && !details.partialPickup()) throw new MarketplaceException(CONFLICT, "Partial pickup is disabled");
        status=next;
    }
    public void delete(long actor) { requireOwner(actor); deleted=true; }
    public void moderate(boolean hide) { hidden=hide; }
    public Long id() { return id; }
    public long sellerId() { return sellerId; }
    public ListingDetails details() { return details; }
    public ListingStatus status() { return status; }
    public boolean hidden() { return hidden; }
    public boolean deleted() { return deleted; }
    public long version() { return version; }
    public Instant createdAt() { return createdAt; }
}
