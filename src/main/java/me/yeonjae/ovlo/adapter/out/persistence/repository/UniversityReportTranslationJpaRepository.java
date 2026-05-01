package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.UniversityReportTranslationJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.entity.UniversityReportTranslationJpaEntity.TranslationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UniversityReportTranslationJpaRepository
        extends JpaRepository<UniversityReportTranslationJpaEntity, TranslationId> {

    Optional<UniversityReportTranslationJpaEntity> findByIdReportIdAndIdLang(Long reportId, String lang);

    @Query("SELECT t FROM UniversityReportTranslationJpaEntity t WHERE t.id.reportId IN :reportIds")
    List<UniversityReportTranslationJpaEntity> findByReportIds(@Param("reportIds") List<Long> reportIds);

    @Query("SELECT t.id.lang FROM UniversityReportTranslationJpaEntity t WHERE t.id.reportId = :reportId")
    List<String> findLangsByReportId(@Param("reportId") Long reportId);
}
