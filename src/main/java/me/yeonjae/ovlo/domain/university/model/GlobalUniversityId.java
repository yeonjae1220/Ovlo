package me.yeonjae.ovlo.domain.university.model;

public record GlobalUniversityId(Long value) {
    public GlobalUniversityId {
        if (value == null || value < 1) throw new IllegalArgumentException("GlobalUniversityId must be positive");
    }
}
