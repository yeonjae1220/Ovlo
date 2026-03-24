package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.ExchangeUniversityJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExchangeUniversityJpaRepository extends JpaRepository<ExchangeUniversityJpaEntity, Long> {

    @Query(value = """
            SELECT * FROM exchange_universities
            WHERE (:keyword IS NULL OR name_ko ILIKE '%' || :keyword || '%' OR name_en ILIKE '%' || :keyword || '%')
              AND (:country IS NULL OR country = :country)
            ORDER BY name_en
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<ExchangeUniversityJpaEntity> search(
            @Param("keyword") String keyword,
            @Param("country") String country,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
            SELECT COUNT(*) FROM exchange_universities
            WHERE (:keyword IS NULL OR name_ko ILIKE '%' || :keyword || '%' OR name_en ILIKE '%' || :keyword || '%')
              AND (:country IS NULL OR country = :country)
            """, nativeQuery = true)
    long countSearch(@Param("keyword") String keyword, @Param("country") String country);
}
