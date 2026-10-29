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
              AND NOT EXISTS (SELECT 1 FROM global_universities h
                              WHERE h.id = eu.global_univ_id AND h.canonical_id IS NOT NULL)
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
            SELECT COUNT(*) FROM exchange_universities eu
            WHERE (:keyword IS NULL OR eu.name_ko ILIKE '%' || :keyword || '%' OR eu.name_en ILIKE '%' || :keyword || '%')
              AND (:countryCode IS NULL OR eu.country_code = :countryCode)
              AND NOT EXISTS (SELECT 1 FROM global_universities h
                              WHERE h.id = eu.global_univ_id AND h.canonical_id IS NOT NULL)
            """, nativeQuery = true)
    long countSearch(@Param("keyword") String keyword, @Param("countryCode") String countryCode);

    @Query(value = """
            SELECT new me.yeonjae.ovlo.application.dto.result.ExchangeUniversityCountryResult(
                e.country, e.countryCode, COUNT(e))
            FROM ExchangeUniversityJpaEntity e
            WHERE e.country IS NOT NULL AND e.countryCode IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM GlobalUniversityJpaEntity h
                              WHERE h.id = e.globalUnivId AND h.canonicalId IS NOT NULL)
            GROUP BY e.country, e.countryCode
            ORDER BY e.country
            """)
    List<ExchangeUniversityCountryResult> findCountries();
}
