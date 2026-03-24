package me.yeonjae.ovlo.domain.university.model;

public record ExchangeUniversityId(Long value) {
    public ExchangeUniversityId {
        if (value == null || value < 1) throw new IllegalArgumentException("ExchangeUniversityId must be positive");
    }
}
