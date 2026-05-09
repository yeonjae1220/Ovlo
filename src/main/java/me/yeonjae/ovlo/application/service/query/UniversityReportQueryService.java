package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.UniversityReportResult;
import me.yeonjae.ovlo.application.dto.result.UniversityReportSummaryResult;
import me.yeonjae.ovlo.application.port.in.university.GetUniversityReportQuery;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityReportPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UniversityReportQueryService implements GetUniversityReportQuery {

    private final LoadUniversityReportPort loadUniversityReportPort;

    public UniversityReportQueryService(LoadUniversityReportPort loadUniversityReportPort) {
        this.loadUniversityReportPort = loadUniversityReportPort;
    }

    @Override
    public PageResult<UniversityReportSummaryResult> getReports(String lang, String keyword, int page, int size) {
        return loadUniversityReportPort.findPageByLang(lang, keyword, page, size);
    }

    @Override
    public Optional<UniversityReportResult> getById(Long id, String lang) {
        return loadUniversityReportPort.findByIdAndLang(id, lang);
    }

    @Override
    public Optional<UniversityReportResult> getByGlobalUnivId(Long globalUnivId, String lang) {
        return loadUniversityReportPort.findByGlobalUnivIdAndLang(globalUnivId, lang);
    }

    @Override
    public Optional<UniversityReportResult> getByExchangeUnivId(Long exchangeUnivId, String lang) {
        return loadUniversityReportPort.findByExchangeUnivIdAndLang(exchangeUnivId, lang);
    }

    @Override
    public List<String> getAvailableLangs(Long reportId) {
        return loadUniversityReportPort.findLangsByReportId(reportId);
    }
}
