package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.university.model.GlobalUniversity;

public record GlobalUniversityResult(
        Long id,
        String nameEn,
        String country,
        String countryEn,
        String countryCode,
        String city,
        String website
) {
    public static GlobalUniversityResult from(GlobalUniversity u) {
        return new GlobalUniversityResult(
                u.getId().value(),
                u.getNameEn(),
                u.getCountry(),
                u.getCountryEn(),
                u.getCountryCode(),
                u.getCity(),
                u.getWebsite()
        );
    }
}
