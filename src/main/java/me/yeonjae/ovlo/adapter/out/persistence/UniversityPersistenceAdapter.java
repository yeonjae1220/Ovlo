package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.mapper.UniversityMapper;
import me.yeonjae.ovlo.adapter.out.persistence.repository.GlobalUniversityJpaRepository;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityPort;
import me.yeonjae.ovlo.application.port.out.university.SearchUniversityPort;
import me.yeonjae.ovlo.application.port.out.university.UniversityDomainLookupPort;
import me.yeonjae.ovlo.domain.university.model.University;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 단일 대학 카탈로그 어댑터. global_universities(약 10,150개)를 백킹으로
 * 조회·검색을 제공한다. (구 university 40-테이블 스택은 폐기됨)
 */
@Component
public class UniversityPersistenceAdapter
        implements LoadUniversityPort, SearchUniversityPort, UniversityDomainLookupPort {

    private final GlobalUniversityJpaRepository repository;
    private final UniversityMapper mapper;

    public UniversityPersistenceAdapter(GlobalUniversityJpaRepository repository,
                                        UniversityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<University> findById(UniversityId id) {
        return repository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(UniversityId id) {
        return repository.existsById(id.value());
    }

    @Override
    public List<University> search(String keyword, String countryCode, int offset, int limit) {
        return repository.search(blankToNull(keyword), blankToNull(countryCode), limit, offset)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long count(String keyword, String countryCode) {
        return repository.countSearch(blankToNull(keyword), blankToNull(countryCode));
    }

    @Override
    public Map<Long, String> findLocalizedNames(Collection<Long> ids, String lang) {
        if (ids == null || ids.isEmpty() || lang == null || lang.isBlank()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        for (Object[] row : repository.findLocalizedNames(ids, lang.trim().toLowerCase())) {
            if (row[1] != null) {
                names.put(((Number) row[0]).longValue(), (String) row[1]);
            }
        }
        return Map.copyOf(names);
    }

    @Override
    public List<University> findByEmailDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            return List.of();
        }
        return repository.findByDomainIgnoreCase(domain.trim())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
