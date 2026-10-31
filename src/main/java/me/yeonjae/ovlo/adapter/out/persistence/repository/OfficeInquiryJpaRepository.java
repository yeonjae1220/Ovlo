package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.OfficeInquiryJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface OfficeInquiryJpaRepository extends JpaRepository<OfficeInquiryJpaEntity, Long> {

    /** from·to 는 null 을 넘기지 않는다 — PostgreSQL 은 null timestamptz 파라미터의 타입을 정하지 못한다. 어댑터가 경계값을 채운다. */

    @Query(value = """
            SELECT i FROM OfficeInquiryJpaEntity i
             WHERE i.officeId = :officeId
               AND (:outcome IS NULL OR i.outcome = :outcome)
               AND i.askedAt >= :from
               AND i.askedAt < :to
            """,
            countQuery = """
            SELECT COUNT(i) FROM OfficeInquiryJpaEntity i
             WHERE i.officeId = :officeId
               AND (:outcome IS NULL OR i.outcome = :outcome)
               AND i.askedAt >= :from
               AND i.askedAt < :to
            """)
    Page<OfficeInquiryJpaEntity> findByOfficeFiltered(@Param("officeId") Long officeId,
                                                      @Param("outcome") String outcome,
                                                      @Param("from") Instant from,
                                                      @Param("to") Instant to,
                                                      Pageable pageable);

    long countByOfficeIdAndAskedAtGreaterThanEqual(Long officeId, Instant since);
}
