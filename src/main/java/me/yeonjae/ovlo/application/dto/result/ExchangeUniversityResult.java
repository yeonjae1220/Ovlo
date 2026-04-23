package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.university.model.ExchangeUniversity;

public record ExchangeUniversityResult(
        Long id,
        String nameKo,
        String nameEn,
        String country,
        String countryCode,
        String city,
        String website,
        Long globalUnivId,
        long reviewCount,
        Double avgRating
) {
    public static ExchangeUniversityResult of(ExchangeUniversity eu, long reviewCount, Double avgRating) {
        return new ExchangeUniversityResult(
                eu.getId().value(),
                eu.getNameKo(),
                eu.getNameEn(),
                eu.getCountry(),
                eu.getCountryCode(),
                eu.getCity(),
                eu.getWebsite(),
                eu.getGlobalUnivId(),
                reviewCount,
                avgRating
        );
    }
}
