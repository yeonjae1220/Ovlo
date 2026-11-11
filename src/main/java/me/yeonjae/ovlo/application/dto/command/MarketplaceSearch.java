package me.yeonjae.ovlo.application.dto.command;
import me.yeonjae.ovlo.domain.marketplace.model.ListingTag;
public record MarketplaceSearch(String country, String city, Long universityId, String keyword, String category,
        String currency, ListingTag tag, Boolean free, Boolean bundle, boolean availableOnly, boolean mine, int page) {}
