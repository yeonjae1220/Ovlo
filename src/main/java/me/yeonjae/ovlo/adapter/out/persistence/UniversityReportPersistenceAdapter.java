package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.entity.UniversityReportJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.entity.UniversityReportTranslationJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.repository.UniversityReportJpaRepository;
import me.yeonjae.ovlo.adapter.out.persistence.repository.UniversityReportTranslationJpaRepository;
import me.yeonjae.ovlo.application.port.out.report.LoadUniversityReportPort;
import me.yeonjae.ovlo.domain.report.model.UniversityReport;
import me.yeonjae.ovlo.domain.report.model.UniversityReportTranslation;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class UniversityReportPersistenceAdapter implements LoadUniversityReportPort {

    private final UniversityReportJpaRepository reportRepo;
    private final UniversityReportTranslationJpaRepository translationRepo;

    public UniversityReportPersistenceAdapter(UniversityReportJpaRepository reportRepo,
                                              UniversityReportTranslationJpaRepository translationRepo) {
        this.reportRepo = reportRepo;
        this.translationRepo = translationRepo;
    }

    @Override
    public Optional<UniversityReport> findById(Long reportId) {
        return reportRepo.findById(reportId).map(this::toDomain);
    }

    @Override
    public Optional<UniversityReport> findByGlobalUnivId(Long globalUnivId) {
        return reportRepo.findByGlobalUnivId(globalUnivId).map(this::toDomain);
    }

    @Override
    public List<UniversityReport> findAllPublished(int offset, int limit) {
        PageRequest pageable = PageRequest.of(offset / limit, limit, Sort.by("id").descending());
        return reportRepo.findByStatus("PUBLISHED", pageable)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long countAllPublished() {
        return reportRepo.countByStatus("PUBLISHED");
    }

    @Override
    public Optional<UniversityReportTranslation> findTranslation(Long reportId, String lang) {
        return translationRepo.findByIdReportIdAndIdLang(reportId, lang)
                .map(this::toTranslationDomain);
    }

    @Override
    public List<String> findSupportedLanguages(Long reportId) {
        return translationRepo.findLangsByReportId(reportId);
    }

    private UniversityReport toDomain(UniversityReportJpaEntity e) {
        List<String> langs = e.getSupportedLangs() != null
                ? Arrays.asList(e.getSupportedLangs())
                : Collections.emptyList();
        return new UniversityReport(
                e.getId(), e.getGlobalUnivId(), e.getExchangeUnivId(), e.getStatus(),
                e.getSourceVideoCount(), e.getSourceWebCount(),
                e.getAvgRating(), e.getRecommendRatio(), e.getAvgDifficulty(),
                e.getCostCurrency(), e.getAggregateStats(),
                langs, e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    private UniversityReportTranslation toTranslationDomain(UniversityReportTranslationJpaEntity e) {
        return new UniversityReportTranslation(
                e.getReportId(), e.getLang(), e.getTitle(),
                e.getSummary(), e.getBody(), e.getContent(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }

}
