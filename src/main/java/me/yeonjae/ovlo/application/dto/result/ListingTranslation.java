package me.yeonjae.ovlo.application.dto.result;
public record ListingTranslation(long listingId, long version, String language, String title, String description, boolean machineTranslated) {}
