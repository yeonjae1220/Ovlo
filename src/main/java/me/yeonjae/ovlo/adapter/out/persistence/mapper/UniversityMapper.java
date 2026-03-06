package me.yeonjae.ovlo.adapter.out.persistence.mapper;

import me.yeonjae.ovlo.adapter.out.persistence.entity.UniversityJpaEntity;
import me.yeonjae.ovlo.domain.university.model.*;
import org.springframework.stereotype.Component;

@Component
public class UniversityMapper {

    public UniversityJpaEntity toJpaEntity(University university) {
        UniversityJpaEntity entity = new UniversityJpaEntity();
        if (university.getId() != null) {
            entity.setId(university.getId().value());
        }
        entity.setName(university.getName());
        entity.setLocalName(university.getLocalName());
        entity.setCountryCode(university.getCountryCode().value());
        entity.setCity(university.getCity());
        entity.setLatitude(university.getGeoLocation().latitude());
        entity.setLongitude(university.getGeoLocation().longitude());
        entity.setWebsiteUrl(university.getWebsiteUrl());
        return entity;
    }

    public University toDomain(UniversityJpaEntity entity) {
        return University.restore(
                new UniversityId(entity.getId()),
                entity.getName(),
                entity.getLocalName(),
                new CountryCode(entity.getCountryCode()),
                entity.getCity(),
                new GeoLocation(entity.getLatitude(), entity.getLongitude()),
                entity.getWebsiteUrl()
        );
    }
}
