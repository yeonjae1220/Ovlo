package me.yeonjae.ovlo.domain.marketplace.model;

import java.time.LocalDate;
import java.util.*;

public record ListingDetails(String title, String description, String sourceLanguage, String category, String condition,
        boolean free, boolean bundle, boolean partialPickup, AskingPrice price, PickupLocation location,
        LocalDate pickupFrom, LocalDate pickupUntil, Set<ListingTag> tags, List<UUID> photoIds) {
    public static final Set<String> LANGUAGES = Set.of("ko", "en", "ja", "zh", "es", "fr", "de", "pt", "ru");
    public static final Set<String> CATEGORIES = Set.of("HOUSEHOLD", "FURNITURE", "ELECTRONICS", "BOOKS", "CLOTHING", "MIXED", "OTHER");
    public static final Set<String> CONDITIONS = Set.of("NEW", "LIKE_NEW", "GOOD", "WORN");
    public ListingDetails {
        title = PickupLocation.text(title, 120, "Title");
        description = PickupLocation.text(description, 5000, "Description");
        if (sourceLanguage == null || !LANGUAGES.contains(sourceLanguage)) throw new IllegalArgumentException("Unsupported source language");
        if (category == null || !CATEGORIES.contains(category) || condition == null || !CONDITIONS.contains(condition)) throw new IllegalArgumentException("Invalid category or condition");
        Objects.requireNonNull(price, "Price required"); Objects.requireNonNull(location, "Location required");
        if (free != (price.amount().signum() == 0)) throw new IllegalArgumentException("Giveaways must be free; sale price must be positive");
        if (partialPickup && !bundle) throw new IllegalArgumentException("Partial pickup requires a bundle");
        if (pickupFrom != null && pickupUntil != null && pickupFrom.isAfter(pickupUntil)) throw new IllegalArgumentException("Pickup dates are reversed");
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        photoIds = photoIds == null ? List.of() : List.copyOf(photoIds);
        if (photoIds.isEmpty() || photoIds.size() > 8 || new HashSet<>(photoIds).size() != photoIds.size()) throw new IllegalArgumentException("Choose 1–8 distinct photos");
    }
}
