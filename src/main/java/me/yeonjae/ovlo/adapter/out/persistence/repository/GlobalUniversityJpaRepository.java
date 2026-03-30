package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.GlobalUniversityJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GlobalUniversityJpaRepository extends JpaRepository<GlobalUniversityJpaEntity, Long> {

    @Query(value = """
            SELECT * FROM global_universities
            WHERE (:keyword IS NULL OR name_en ILIKE '%' || :keyword || '%')
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
            WHERE (:keyword IS NULL OR name_en ILIKE '%' || :keyword || '%')
              AND (:countryCode IS NULL OR country_code = :countryCode)
            """, nativeQuery = true)
    long countSearch(@Param("keyword") String keyword, @Param("countryCode") String countryCode);
}
