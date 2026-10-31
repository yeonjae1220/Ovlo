package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.PartnerClickJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PartnerClickJpaRepository extends JpaRepository<PartnerClickJpaEntity, Long> {

    /** [category, count] */
    @Query("""
            SELECT p.category, COUNT(c) FROM PartnerClickJpaEntity c JOIN PartnerJpaEntity p ON p.id = c.partnerId
             WHERE c.officeId = :officeId AND c.clickedAt >= :from AND c.clickedAt < :to
             GROUP BY p.category
            """)
    List<Object[]> countByCategory(@Param("officeId") Long officeId, @Param("from") Instant from, @Param("to") Instant to);
}
