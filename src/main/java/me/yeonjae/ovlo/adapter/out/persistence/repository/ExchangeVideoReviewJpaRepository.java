package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.ExchangeVideoReviewJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExchangeVideoReviewJpaRepository extends JpaRepository<ExchangeVideoReviewJpaEntity, Long> {

    @Query(value = """
            SELECT * FROM exchange_video_reviews
            WHERE university_id = :universityId
            ORDER BY overall_rating DESC NULLS LAST, quality_score DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<ExchangeVideoReviewJpaEntity> findByUniversityId(
            @Param("universityId") Long universityId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
            SELECT * FROM exchange_video_reviews
            WHERE university_id = :universityId
              AND (:direction IS NULL OR direction = :direction)
            ORDER BY overall_rating DESC NULLS LAST, quality_score DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<ExchangeVideoReviewJpaEntity> findByUniversityIdAndDirection(
            @Param("universityId") Long universityId,
            @Param("direction") String direction,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long countByUniversityId(Long universityId);

    @Query(value = """
            SELECT COUNT(*) FROM exchange_video_reviews
            WHERE university_id = :universityId
              AND (:direction IS NULL OR direction = :direction)
            """, nativeQuery = true)
    long countByUniversityIdAndDirection(
            @Param("universityId") Long universityId,
            @Param("direction") String direction
    );

    @Query(value = """
            SELECT university_id AS universityId, COUNT(*) AS reviewCount
            FROM exchange_video_reviews
            WHERE university_id IN :ids
            GROUP BY university_id
            """, nativeQuery = true)
    List<ReviewCountProjection> countByUniversityIdIn(@Param("ids") List<Long> ids);
}
