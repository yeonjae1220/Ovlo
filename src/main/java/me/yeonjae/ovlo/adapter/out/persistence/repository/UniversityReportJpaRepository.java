package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.UniversityReportJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UniversityReportJpaRepository extends JpaRepository<UniversityReportJpaEntity, Long> {

    Optional<UniversityReportJpaEntity> findByGlobalUnivId(Long globalUnivId);

    @Query(value = """
            SELECT r.* FROM university_report r
            WHERE r.status = 'PUBLISHED'
              AND (:keyword IS NULL OR :keyword = ''
                   OR r.global_univ_id IN (
                       SELECT id FROM global_universities
                       WHERE LOWER(name_en) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   )
                  )
            ORDER BY r.source_video_count DESC, r.id ASC
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<UniversityReportJpaEntity> findPublishedPage(
            @Param("keyword") String keyword,
            @Param("size") int size,
            @Param("offset") int offset);

    @Query(value = """
            SELECT COUNT(*) FROM university_report
            WHERE status = 'PUBLISHED'
              AND (:keyword IS NULL OR :keyword = ''
                   OR global_univ_id IN (
                       SELECT id FROM global_universities
                       WHERE LOWER(name_en) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   )
                  )
            """, nativeQuery = true)
    long countPublished(@Param("keyword") String keyword);
}
