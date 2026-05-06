package me.yeonjae.ovlo.application.port.out.university;

import me.yeonjae.ovlo.application.dto.result.UniversityReportResult;
import me.yeonjae.ovlo.application.dto.result.UniversityReportSummaryResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;

import java.util.List;
import java.util.Optional;

public interface LoadUniversityReportPort {
    PageResult<UniversityReportSummaryResult> findPageByLang(String lang, String keyword, int page, int size);
    Optional<UniversityReportResult> findByIdAndLang(Long id, String lang);
    Optional<UniversityReportResult> findByGlobalUnivIdAndLang(Long globalUnivId, String lang);
    Optional<UniversityReportResult> findByExchangeUnivIdAndLang(Long exchangeUnivId, String lang);
    List<String> findLangsByReportId(Long reportId);
}
