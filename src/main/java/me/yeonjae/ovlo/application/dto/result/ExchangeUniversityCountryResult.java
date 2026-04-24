package me.yeonjae.ovlo.application.dto.result;

public record ExchangeUniversityCountryResult(
        String country,
        String countryCode,
        long universityCount
) {}
