package me.yeonjae.ovlo.application.port.in.university;

import me.yeonjae.ovlo.application.dto.result.UniversityReportResult;
import me.yeonjae.ovlo.application.dto.result.UniversityReportSummaryResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;

import java.util.List;
import java.util.Optional;

public interface GetUniversityReportQuery {
    PageResult<UniversityReportSummaryResult> getReports(String lang, String keyword, int page, int size);
    Optional<UniversityReportResult> getById(Long id, String lang);
    Optional<UniversityReportResult> getByGlobalUnivId(Long globalUnivId, String lang);
    Optional<UniversityReportResult> getByExchangeUnivId(Long exchangeUnivId, String lang);
    List<String> getAvailableLangs(Long reportId);
}
