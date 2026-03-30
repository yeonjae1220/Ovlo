package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.entity.GlobalUniversityJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.repository.GlobalUniversityJpaRepository;
import me.yeonjae.ovlo.application.port.out.university.LoadGlobalUniversityPort;
import me.yeonjae.ovlo.domain.university.model.GlobalUniversity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GlobalUniversityPersistenceAdapter implements LoadGlobalUniversityPort {

    private final GlobalUniversityJpaRepository globalUniversityJpaRepository;

    public GlobalUniversityPersistenceAdapter(GlobalUniversityJpaRepository globalUniversityJpaRepository) {
        this.globalUniversityJpaRepository = globalUniversityJpaRepository;
    }

    @Override
    public List<GlobalUniversity> search(String keyword, String countryCode, int offset, int limit) {
        return globalUniversityJpaRepository
                .search(blankToNull(keyword), blankToNull(countryCode), limit, offset)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long count(String keyword, String countryCode) {
        return globalUniversityJpaRepository.countSearch(blankToNull(keyword), blankToNull(countryCode));
    }

    private GlobalUniversity toDomain(GlobalUniversityJpaEntity e) {
        return GlobalUniversity.restore(
                e.getId(), e.getNameEn(), e.getCountry(),
                e.getCountryEn(), e.getCountryCode(),
                e.getCity(), e.getWebsite(), e.getDomain()
        );
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
