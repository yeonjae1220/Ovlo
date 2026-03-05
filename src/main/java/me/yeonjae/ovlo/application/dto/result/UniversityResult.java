package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.university.model.University;

public record UniversityResult(
        Long id,
        String name,
        String localName,
        String countryCode,
        String city,
        double latitude,
        double longitude,
        String websiteUrl
) {
    public static UniversityResult from(University university) {
        return new UniversityResult(
                university.getId() != null ? university.getId().value() : null,
                university.getName(),
                university.getLocalName(),
                university.getCountryCode().value(),
                university.getCity(),
                university.getGeoLocation().latitude(),
                university.getGeoLocation().longitude(),
                university.getWebsiteUrl()
        );
    }
}
