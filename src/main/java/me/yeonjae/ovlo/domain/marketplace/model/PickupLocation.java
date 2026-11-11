package me.yeonjae.ovlo.domain.marketplace.model;

import java.util.*;

public record PickupLocation(String countryCode, String city, Long universityId, String meetingPoint) {
    public PickupLocation {
        if (countryCode == null || !Set.of(Locale.getISOCountries()).contains(countryCode.toUpperCase(Locale.ROOT)))
            throw new IllegalArgumentException("Select a country");
        countryCode = countryCode.toUpperCase(Locale.ROOT);
        city = text(city, 100, "City"); meetingPoint = text(meetingPoint, 200, "Pickup location");
        if (universityId != null && universityId <= 0) throw new IllegalArgumentException("Invalid university");
    }
    public static String text(String value, int max, String label) {
        if (value == null || value.isBlank() || value.trim().length() > max) throw new IllegalArgumentException(label + " is required (max " + max + ")");
        return value.trim();
    }
}
