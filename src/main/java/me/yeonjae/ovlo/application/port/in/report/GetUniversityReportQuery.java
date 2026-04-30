package me.yeonjae.ovlo.application.port.in.report;

import me.yeonjae.ovlo.application.dto.result.UniversityReportPageResult;
import me.yeonjae.ovlo.application.dto.result.UniversityReportResult;

import java.util.List;

public interface GetUniversityReportQuery {
    UniversityReportResult getById(Long reportId, String lang);
    UniversityReportResult getByGlobalUnivId(Long globalUnivId, String lang);
    UniversityReportPageResult list(String lang, int page, int size);
    List<String> getSupportedLanguages(Long reportId);
}
