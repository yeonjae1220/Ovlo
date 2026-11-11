package me.yeonjae.ovlo.application.dto.result;
import me.yeonjae.ovlo.domain.marketplace.model.*;
import java.time.Instant;
public record ListingResult(long id, long sellerId, String sellerName, String universityName,
        ListingDetails details, ListingStatus status, boolean hidden, long version, Instant createdAt) {}
