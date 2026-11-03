package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.university.model.University;

/**
 * 전세계 대학 검색 응답 (프론트 /api/v1/global-universities 계약).
 * 단일 University 도메인을 백킹으로 하되, 기존 프론트 필드명(nameEn/website 등)을 유지한다.
 * {@code localName} 은 요청한 표시 언어의 대학명(없으면 null) — 드롭다운 보조 표기용.
 */
public record GlobalUniversityResult(
        Long id,
        String nameEn,
        String localName,
        String country,
        String countryEn,
        String countryCode,
        String city,
        String website,
        Double latitude,
        Double longitude
) {
    public static GlobalUniversityResult from(University u, String localName) {
        return new GlobalUniversityResult(
                u.getId().value(),
                u.getName(),
                localName,
                u.getCountry(),
                u.getCountryEn(),
                u.getCountryCode() != null ? u.getCountryCode().value() : null,
                u.getCity(),
                u.getWebsiteUrl(),
                u.getGeoLocation() != null ? u.getGeoLocation().latitude() : null,
                u.getGeoLocation() != null ? u.getGeoLocation().longitude() : null
        );
    }
}
