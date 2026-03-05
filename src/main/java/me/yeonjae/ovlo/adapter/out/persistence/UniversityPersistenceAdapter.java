package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.port.out.university.LoadUniversityPort;
import me.yeonjae.ovlo.application.port.out.university.SearchUniversityPort;
import me.yeonjae.ovlo.domain.university.model.University;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * TODO: UniversityJpaRepository + QueryDSL 구현으로 교체 예정
 */
@Component
public class UniversityPersistenceAdapter implements LoadUniversityPort, SearchUniversityPort {

    @Override
    public Optional<University> findById(UniversityId id) {
        throw new UnsupportedOperationException("JPA 구현 예정");
    }

    @Override
    public List<University> search(String keyword, String countryCode, int offset, int limit) {
        throw new UnsupportedOperationException("JPA 구현 예정");
    }

    @Override
    public long count(String keyword, String countryCode) {
        throw new UnsupportedOperationException("JPA 구현 예정");
    }
}
