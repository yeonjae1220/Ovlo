package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.OfficeConsultationJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OfficeConsultationJpaRepository extends JpaRepository<OfficeConsultationJpaEntity, Long> {

    Optional<OfficeConsultationJpaEntity> findByChatRoomId(Long chatRoomId);

    @Query(value = """
            SELECT c FROM OfficeConsultationJpaEntity c
             WHERE c.officeId = :officeId AND (:status IS NULL OR c.status = :status)
            """,
            countQuery = """
            SELECT COUNT(c) FROM OfficeConsultationJpaEntity c
             WHERE c.officeId = :officeId AND (:status IS NULL OR c.status = :status)
            """)
    Page<OfficeConsultationJpaEntity> findByOfficeFiltered(@Param("officeId") Long officeId,
                                                           @Param("status") String status, Pageable pageable);

    List<OfficeConsultationJpaEntity> findByOfficeIdAndMemberIdOrderByOpenedAtDesc(Long officeId, Long memberId);

    List<OfficeConsultationJpaEntity> findByOfficeIdAndStatusNot(Long officeId, String status);

    @Query("SELECT c.id FROM OfficeConsultationJpaEntity c WHERE c.status = 'RESOLVED' AND c.resolvedAt <= :cutoff ORDER BY c.id")
    List<Long> findIdsResolvedBefore(@Param("cutoff") Instant cutoff);
}
