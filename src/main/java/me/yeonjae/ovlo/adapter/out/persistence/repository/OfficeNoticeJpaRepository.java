package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.OfficeNoticeJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OfficeNoticeJpaRepository extends JpaRepository<OfficeNoticeJpaEntity, Long> {

    @EntityGraph(attributePaths = "translations")
    Optional<OfficeNoticeJpaEntity> findWithTranslationsById(Long id);

    /** 담당자 목록. status/category 가 null 이면 필터하지 않는다. */
    @Query(value = """
            SELECT n FROM OfficeNoticeJpaEntity n
             WHERE n.officeId = :officeId
               AND (:status IS NULL OR n.status = :status)
               AND (:category IS NULL OR n.category = :category)
            """,
            countQuery = """
            SELECT COUNT(n) FROM OfficeNoticeJpaEntity n
             WHERE n.officeId = :officeId
               AND (:status IS NULL OR n.status = :status)
               AND (:category IS NULL OR n.category = :category)
            """)
    Page<OfficeNoticeJpaEntity> findByOfficeFiltered(@Param("officeId") Long officeId,
                                                     @Param("status") String status,
                                                     @Param("category") String category,
                                                     Pageable pageable);

    /** 학생 조회·근거 후보: APPROVED 이고 오늘이 기간 안인 공지. 번역은 함께 읽는다. */
    @Query("""
            SELECT DISTINCT n FROM OfficeNoticeJpaEntity n
              LEFT JOIN FETCH n.translations
             WHERE n.officeId = :officeId
               AND n.status = 'APPROVED'
               AND n.effectiveFrom <= :today
               AND n.expiresAt >= :today
               AND (:category IS NULL OR n.category = :category)
             ORDER BY n.expiresAt ASC, n.id DESC
            """)
    List<OfficeNoticeJpaEntity> findActiveForStudent(@Param("officeId") Long officeId,
                                                     @Param("category") String category,
                                                     @Param("today") LocalDate today);

    /** 만료 스케줄러 대상 id. 건별 트랜잭션으로 처리하므로 id 만 가져온다(GLOBAL-PIT-128). */
    @Query("SELECT n.id FROM OfficeNoticeJpaEntity n WHERE n.status = 'APPROVED' AND n.expiresAt < :today ORDER BY n.id")
    List<Long> findIdsToExpire(@Param("today") LocalDate today);

    /** 만료 임박 알림 대상 id: APPROVED, 아직 안 알림, 만료일이 [today, today+days]. */
    @Query("""
            SELECT n.id FROM OfficeNoticeJpaEntity n
             WHERE n.status = 'APPROVED'
               AND n.expiryWarnedAt IS NULL
               AND n.expiresAt >= :today
               AND n.expiresAt <= :until
             ORDER BY n.id
            """)
    List<Long> findIdsExpiringBetween(@Param("today") LocalDate today, @Param("until") LocalDate until);
}
