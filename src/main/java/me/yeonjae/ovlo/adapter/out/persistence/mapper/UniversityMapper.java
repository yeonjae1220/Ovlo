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
        CountryCode countryCode = (e.getCountryCode() != null && !e.getCountryCode().isBlank())
                ? new CountryCode(e.getCountryCode())
                : null;

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
}
