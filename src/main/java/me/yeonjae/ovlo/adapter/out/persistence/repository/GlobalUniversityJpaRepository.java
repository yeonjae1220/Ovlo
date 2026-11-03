package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.GlobalUniversityJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GlobalUniversityJpaRepository extends JpaRepository<GlobalUniversityJpaEntity, Long> {

    /** 리포트 통화 환산용 대학 소재국 코드 조회. */
    @Query(value = "SELECT country_code FROM global_universities WHERE id = :id", nativeQuery = true)
    Optional<String> findCountryCodeById(@Param("id") Long id);

    /**
     * 본교 선택 검색의 키워드 조건(행 별칭 g). 영문명뿐 아니라 현지어명(local_name)·
     * 다국어 이름(global_university_names)·연결된 교환대학의 한글명(exchange_universities.name_ko)
     * 으로도 찾는다 — 회원가입·온보딩에서 "서울대"처럼 모국어로 쳐도 나오게.
     * 숨긴 중복 행(canonical_id 보유)의 이름이 맞으면 그 대표 행을 돌려준다.
     */
    String KEYWORD_MATCH = """
            (:keyword IS NULL
             OR g.name_en ILIKE '%' || :keyword || '%'
             OR g.local_name ILIKE '%' || :keyword || '%'
             OR EXISTS (SELECT 1 FROM global_university_names gn
                        WHERE gn.global_univ_id = g.id
                          AND gn.name ILIKE '%' || :keyword || '%')
             OR EXISTS (SELECT 1 FROM exchange_universities e
                        WHERE e.global_univ_id = g.id
                          AND e.name_ko ILIKE '%' || :keyword || '%')
             OR g.id IN (SELECT h.canonical_id FROM global_universities h
                         WHERE h.canonical_id IS NOT NULL
                           AND (h.name_en ILIKE '%' || :keyword || '%'
                                OR h.local_name ILIKE '%' || :keyword || '%'
                                OR EXISTS (SELECT 1 FROM global_university_names hn
                                           WHERE hn.global_univ_id = h.id
                                             AND hn.name ILIKE '%' || :keyword || '%'))))
            """;

    /**
     * 본교 선택 검색. 중복으로 숨긴 행(canonical_id 보유)은 제외하되,
     * 숨긴 행의 옛 이름이 키워드에 맞으면 그 대표 행을 돌려준다.
     */
    @Query(value = "SELECT g.* FROM global_universities g "
            + "WHERE g.canonical_id IS NULL "
            + "  AND " + KEYWORD_MATCH
            + "  AND (:countryCode IS NULL OR g.country_code = :countryCode) "
            + "ORDER BY g.name_en "
            + "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<GlobalUniversityJpaEntity> search(
            @Param("keyword") String keyword,
            @Param("countryCode") String countryCode,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = "SELECT COUNT(*) FROM global_universities g "
            + "WHERE g.canonical_id IS NULL "
            + "  AND " + KEYWORD_MATCH
            + "  AND (:countryCode IS NULL OR g.country_code = :countryCode)",
            nativeQuery = true)
    long countSearch(@Param("keyword") String keyword, @Param("countryCode") String countryCode);

    /**
     * 이메일 도메인으로 대학 역조회 (학교 이메일 검증용).
     * 데이터셋상 동일 domain을 가진 별개 대학이 존재할 수 있어 List 반환(1:N).
     * 중복으로 숨긴 행의 domain 이 맞으면 그 행 대신 대표 행(canonical_id)을 돌려준다.
     */
    @Query(value = """
            SELECT * FROM global_universities
            WHERE id IN (SELECT COALESCE(canonical_id, id) FROM global_universities
                         WHERE lower(domain) = lower(:domain))
            """, nativeQuery = true)
    List<GlobalUniversityJpaEntity> findByDomainIgnoreCase(@Param("domain") String domain);
}
