package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.GlobalUniversityJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 콘텐츠(리포트/후기) 보유 대학 통합 검색용 네이티브 쿼리.
 *
 * <p>집합 = (global_universities ⋈ 리포트/후기) ∪ (글로벌 미연결 후기 대학).
 * LATERAL + LIMIT 1 로 글로벌 대학당 정확히 한 행을 보장 → 검색/카운트 정합성 유지.</p>
 *
 * <p>엔티티 타입은 형식상 {@link GlobalUniversityJpaEntity} 를 지정하되 실제로는
 * {@code Object[]} 행으로 조회한다(컬럼 순서 고정, 어댑터에서 수동 매핑).</p>
 */
public interface UniversityCatalogJpaRepository extends JpaRepository<GlobalUniversityJpaEntity, Long> {

    String CONTENT_SET = """
            SELECT gu.id                                  AS global_univ_id,
                   eu.id                                  AS exchange_univ_id,
                   r.id                                   AS report_id,
                   gu.name_en                             AS name_en,
                   COALESCE(eu.name_ko, gu.local_name)    AS name_ko,
                   COALESCE(gu.country, eu.country)       AS country,
                   COALESCE(gu.country_code, eu.country_code) AS country_code,
                   COALESCE(gu.city, eu.city)             AS city
            FROM global_universities gu
            LEFT JOIN university_report r
                   ON r.global_univ_id = gu.id AND r.status = 'PUBLISHED'
            LEFT JOIN LATERAL (
                   SELECT e.id, e.name_ko, e.country, e.country_code, e.city
                   FROM exchange_universities e
                   WHERE e.global_univ_id = gu.id
                   LIMIT 1
            ) eu ON true
            WHERE (r.id IS NOT NULL OR eu.id IS NOT NULL)
            UNION ALL
            SELECT NULL              AS global_univ_id,
                   eu.id             AS exchange_univ_id,
                   NULL              AS report_id,
                   eu.name_en        AS name_en,
                   eu.name_ko        AS name_ko,
                   eu.country        AS country,
                   eu.country_code   AS country_code,
                   eu.city           AS city
            FROM exchange_universities eu
            WHERE eu.global_univ_id IS NULL
            """;

    @Query(value = "SELECT t.global_univ_id, t.exchange_univ_id, t.report_id, "
            + "t.name_en, t.name_ko, t.country, t.country_code, t.city "
            + "FROM (" + CONTENT_SET + ") t "
            + "WHERE (:keyword IS NULL "
            + "       OR t.name_en ILIKE '%' || :keyword || '%' "
            + "       OR t.name_ko ILIKE '%' || :keyword || '%') "
            + "  AND (:countryCode IS NULL OR t.country_code = :countryCode) "
            + "ORDER BY (t.report_id IS NOT NULL) DESC, "
            + "         (t.exchange_univ_id IS NOT NULL) DESC, "
            + "         t.name_en NULLS LAST "
            + "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<Object[]> search(@Param("keyword") String keyword,
                          @Param("countryCode") String countryCode,
                          @Param("limit") int limit,
                          @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM (" + CONTENT_SET + ") t "
            + "WHERE (:keyword IS NULL "
            + "       OR t.name_en ILIKE '%' || :keyword || '%' "
            + "       OR t.name_ko ILIKE '%' || :keyword || '%') "
            + "  AND (:countryCode IS NULL OR t.country_code = :countryCode)",
            nativeQuery = true)
    long countSearch(@Param("keyword") String keyword,
                     @Param("countryCode") String countryCode);
}
