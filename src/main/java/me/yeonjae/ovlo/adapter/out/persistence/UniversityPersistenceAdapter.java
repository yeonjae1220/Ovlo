package me.yeonjae.ovlo.adapter.out.persistence;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import me.yeonjae.ovlo.adapter.out.persistence.entity.QUniversityJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.mapper.UniversityMapper;
import me.yeonjae.ovlo.adapter.out.persistence.repository.UniversityJpaRepository;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityPort;
import me.yeonjae.ovlo.application.port.out.university.SearchUniversityPort;
import me.yeonjae.ovlo.domain.university.model.University;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UniversityPersistenceAdapter implements LoadUniversityPort, SearchUniversityPort {

    private final UniversityJpaRepository universityJpaRepository;
    private final UniversityMapper universityMapper;
    private final JPAQueryFactory queryFactory;

    public UniversityPersistenceAdapter(UniversityJpaRepository universityJpaRepository,
                                        UniversityMapper universityMapper,
                                        JPAQueryFactory queryFactory) {
        this.universityJpaRepository = universityJpaRepository;
        this.universityMapper = universityMapper;
        this.queryFactory = queryFactory;
    }

    @Override
    public Optional<University> findById(UniversityId id) {
        return universityJpaRepository.findById(id.value()).map(universityMapper::toDomain);
    }

    @Override
    public List<University> search(String keyword, String countryCode, int offset, int limit) {
        QUniversityJpaEntity q = QUniversityJpaEntity.universityJpaEntity;
        return queryFactory.selectFrom(q)
                .where(buildPredicate(q, keyword, countryCode))
                .orderBy(q.name.asc())
                .offset(offset)
                .limit(limit)
                .fetch()
                .stream()
                .map(universityMapper::toDomain)
                .toList();
    }

    @Override
    public long count(String keyword, String countryCode) {
        QUniversityJpaEntity q = QUniversityJpaEntity.universityJpaEntity;
        Long result = queryFactory.select(q.count()).from(q).where(buildPredicate(q, keyword, countryCode)).fetchOne();
        return result != null ? result : 0L;
    }

    private BooleanBuilder buildPredicate(QUniversityJpaEntity q, String keyword, String countryCode) {
        BooleanBuilder predicate = new BooleanBuilder();
        if (keyword != null && !keyword.isBlank()) {
            predicate.and(q.name.containsIgnoreCase(keyword).or(q.localName.containsIgnoreCase(keyword)));
        }
        if (countryCode != null && !countryCode.isBlank()) {
            predicate.and(q.countryCode.eq(countryCode));
        }
        return predicate;
    }
}
