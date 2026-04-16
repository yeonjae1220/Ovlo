package me.yeonjae.ovlo.adapter.in.web.dto.response;

import me.yeonjae.ovlo.domain.university.model.University;

public record AdminUniversityResponse(
        Long id,
        String name,
        String localName,
        String countryCode,
        String city
) {
    public static AdminUniversityResponse of(University university) {
        return new AdminUniversityResponse(
                university.getId() != null ? university.getId().value() : null,
                university.getName(),
                university.getLocalName(),
                university.getCountryCode().value(),
                university.getCity()
        );
    }
}
