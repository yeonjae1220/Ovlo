package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.ExchangeUniversityJpaEntity;
import me.yeonjae.ovlo.application.dto.result.ExchangeUniversityCountryResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExchangeUniversityJpaRepository extends JpaRepository<ExchangeUniversityJpaEntity, Long> {

    @Query(value = """
            SELECT eu.* FROM exchange_universities eu
            LEFT JOIN (
                SELECT university_id, COUNT(*) AS review_count
                FROM exchange_video_reviews
                GROUP BY university_id
            ) rc ON rc.university_id = eu.id
            WHERE (:keyword IS NULL OR eu.name_ko ILIKE '%' || :keyword || '%' OR eu.name_en ILIKE '%' || :keyword || '%')
              AND (:countryCode IS NULL OR eu.country_code = :countryCode)
            ORDER BY COALESCE(rc.review_count, 0) DESC, eu.name_en
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<ExchangeUniversityJpaEntity> search(
            @Param("keyword") String keyword,
            @Param("countryCode") String countryCode,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
            SELECT COUNT(*) FROM exchange_universities
            WHERE (:keyword IS NULL OR name_ko ILIKE '%' || :keyword || '%' OR name_en ILIKE '%' || :keyword || '%')
              AND (:countryCode IS NULL OR country_code = :countryCode)
            """, nativeQuery = true)
    long countSearch(@Param("keyword") String keyword, @Param("countryCode") String countryCode);

    @Query(value = """
            SELECT new me.yeonjae.ovlo.application.dto.result.ExchangeUniversityCountryResult(
                e.country, e.countryCode, COUNT(e))
            FROM ExchangeUniversityJpaEntity e
            WHERE e.country IS NOT NULL AND e.countryCode IS NOT NULL
            GROUP BY e.country, e.countryCode
            ORDER BY e.country
            """)
    List<ExchangeUniversityCountryResult> findCountries();
}
