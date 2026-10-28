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

    /** 본교 선택 검색. 중복으로 숨긴 행(canonical_id 보유)은 제외한다. */
    @Query(value = """
            SELECT * FROM global_universities
            WHERE canonical_id IS NULL
              AND (:keyword IS NULL OR name_en ILIKE '%' || :keyword || '%')
              AND (:countryCode IS NULL OR country_code = :countryCode)
            ORDER BY name_en
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<GlobalUniversityJpaEntity> search(
            @Param("keyword") String keyword,
            @Param("countryCode") String countryCode,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
            SELECT COUNT(*) FROM global_universities
            WHERE canonical_id IS NULL
              AND (:keyword IS NULL OR name_en ILIKE '%' || :keyword || '%')
              AND (:countryCode IS NULL OR country_code = :countryCode)
            """, nativeQuery = true)
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
