package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.UniversityReportJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UniversityReportJpaRepository extends JpaRepository<UniversityReportJpaEntity, Long> {
    Optional<UniversityReportJpaEntity> findByGlobalUnivId(Long globalUnivId);
    List<UniversityReportJpaEntity> findByStatus(String status, Pageable pageable);
    long countByStatus(String status);
}
