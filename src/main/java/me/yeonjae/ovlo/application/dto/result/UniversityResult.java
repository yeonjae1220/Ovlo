package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.university.model.University;

public record UniversityResult(
        Long id,
        String name,
        String localName,
        String countryCode,
        String city,
        Double latitude,
        Double longitude,
        String websiteUrl,
        String domain
) {
    public static UniversityResult from(University u) {
        return new UniversityResult(
                u.getId() != null ? u.getId().value() : null,
                u.getName(),
                u.getLocalName(),
                u.getCountryCode() != null ? u.getCountryCode().value() : null,
                u.getCity(),
                u.getGeoLocation() != null ? u.getGeoLocation().latitude() : null,
                u.getGeoLocation() != null ? u.getGeoLocation().longitude() : null,
                u.getWebsiteUrl(),
                u.getDomain()
        );
    }
}
