package me.yeonjae.ovlo.application.port.in.marketplace;

import me.yeonjae.ovlo.application.dto.command.MarketplaceSearch;
import me.yeonjae.ovlo.application.dto.result.*;
import me.yeonjae.ovlo.domain.marketplace.model.*;
import me.yeonjae.ovlo.application.port.out.marketplace.MarketplaceStore.Report;
import java.util.*;

public interface MarketplaceUseCase {
    record ListingPage(List<ListingResult> content, boolean hasNext) {}
    record BlockedMemberPage(List<BlockedMarketplaceMember> content, Long nextBefore) {}
    BlockedMemberPage blockedMembers(long actor, Long before);
    ListingPage search(long actor, MarketplaceSearch search);
    ListingResult get(long actor, long id);
    ListingResult create(long actor, ListingDetails details);
    ListingResult edit(long actor, long id, long version, ListingDetails details);
    ListingResult status(long actor, long id, long version, ListingStatus status);
    void delete(long actor, long id, long version);
    UUID upload(long actor, byte[] image);
    byte[] photo(long actor, UUID photoId);
    long contact(long actor, long listingId);
    void report(long actor, long listingId, String reason);
    void block(long actor, long target, boolean blocked);
    List<Report> reports();
    void moderate(String admin, long listingId, long version, boolean hidden);
}
