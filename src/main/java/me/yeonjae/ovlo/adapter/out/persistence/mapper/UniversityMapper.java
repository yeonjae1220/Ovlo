package me.yeonjae.ovlo.adapter.out.persistence.mapper;

import me.yeonjae.ovlo.adapter.out.persistence.entity.GlobalUniversityJpaEntity;
import me.yeonjae.ovlo.domain.university.model.*;
import org.springframework.stereotype.Component;

/**
 * global_universities 엔티티 → University 도메인 매핑.
 * 카탈로그는 외부 적재이므로 영속화(toJpaEntity)는 제공하지 않는다.
 */
@Component
public class UniversityMapper {

    public University toDomain(GlobalUniversityJpaEntity e) {
        // 외부 적재(약 10k) 카탈로그라 형식 위반 country_code가 섞일 수 있어
        // VO 검증 실패 시 500이 아니라 null로 관대하게 처리한다.
        CountryCode countryCode = toCountryCodeOrNull(e.getCountryCode());

        GeoLocation geoLocation = (e.getLatitude() != null && e.getLongitude() != null)
                ? new GeoLocation(e.getLatitude(), e.getLongitude())
                : null;

        return University.restore(
                new UniversityId(e.getId()),
                e.getNameEn(),
                e.getLocalName(),
                e.getCountry(),
                e.getCountryEn(),
                countryCode,
                e.getCity(),
                geoLocation,
                e.getWebsite(),
                e.getDomain()
        );
    }

    private CountryCode toCountryCodeOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new CountryCode(raw);
        } catch (IllegalArgumentException ex) {
            return null; // 형식 위반 카탈로그 데이터는 조회를 막지 않고 무시
        }
    }
}
