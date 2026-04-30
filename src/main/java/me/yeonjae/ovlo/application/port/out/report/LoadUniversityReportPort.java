package me.yeonjae.ovlo.application.port.out.report;

import me.yeonjae.ovlo.domain.report.model.UniversityReport;
import me.yeonjae.ovlo.domain.report.model.UniversityReportTranslation;

import java.util.List;
import java.util.Optional;

public interface LoadUniversityReportPort {
    Optional<UniversityReport> findById(Long reportId);
    Optional<UniversityReport> findByGlobalUnivId(Long globalUnivId);
    List<UniversityReport> findAllPublished(int offset, int limit);
    long countAllPublished();
    Optional<UniversityReportTranslation> findTranslation(Long reportId, String lang);
    List<String> findSupportedLanguages(Long reportId);
}
