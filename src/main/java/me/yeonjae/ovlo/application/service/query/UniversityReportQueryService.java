package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.UniversityReportPageResult;
import me.yeonjae.ovlo.application.dto.result.UniversityReportResult;
import me.yeonjae.ovlo.application.port.in.report.GetUniversityReportQuery;
import me.yeonjae.ovlo.application.port.out.report.LoadUniversityReportPort;
import me.yeonjae.ovlo.domain.report.model.UniversityReport;
import me.yeonjae.ovlo.domain.report.model.UniversityReportTranslation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class UniversityReportQueryService implements GetUniversityReportQuery {

    private static final String DEFAULT_LANG = "ko";

    private final LoadUniversityReportPort loadPort;

    public UniversityReportQueryService(LoadUniversityReportPort loadPort) {
        this.loadPort = loadPort;
    }

    @Override
    public UniversityReportResult getById(Long reportId, String lang) {
        UniversityReport report = loadPort.findById(reportId)
                .orElseThrow(() -> new NoSuchElementException("University report not found: " + reportId));
        UniversityReportTranslation translation = resolveTranslation(reportId, lang);
        return UniversityReportResult.from(report, translation);
    }

    @Override
    public UniversityReportResult getByGlobalUnivId(Long globalUnivId, String lang) {
        UniversityReport report = loadPort.findByGlobalUnivId(globalUnivId)
                .orElseThrow(() -> new NoSuchElementException("University report not found for globalUnivId: " + globalUnivId));
        UniversityReportTranslation translation = resolveTranslation(report.getId(), lang);
        return UniversityReportResult.from(report, translation);
    }

    @Override
    public UniversityReportPageResult list(String lang, int page, int size) {
        int offset = page * size;
        List<UniversityReport> reports = loadPort.findAllPublished(offset, size);
        long total = loadPort.countAllPublished();

        List<Long> reportIds = reports.stream().map(UniversityReport::getId).toList();

        List<UniversityReportResult> content = reports.stream().map(report -> {
            UniversityReportTranslation t = resolveTranslation(report.getId(), lang);
            return UniversityReportResult.summary(report, t);
        }).toList();

        return UniversityReportPageResult.of(content, total, page, size);
    }

    @Override
    public List<String> getSupportedLanguages(Long reportId) {
        return loadPort.findSupportedLanguages(reportId);
    }

    private UniversityReportTranslation resolveTranslation(Long reportId, String lang) {
        // 요청 언어 → 없으면 ko → 없으면 아무 언어
        return loadPort.findTranslation(reportId, lang != null ? lang : DEFAULT_LANG)
                .or(() -> loadPort.findTranslation(reportId, DEFAULT_LANG))
                .or(() -> {
                    List<String> langs = loadPort.findSupportedLanguages(reportId);
                    return langs.isEmpty() ? java.util.Optional.empty()
                            : loadPort.findTranslation(reportId, langs.get(0));
                })
                .orElseThrow(() -> new NoSuchElementException("No translation found for report: " + reportId));
    }
}
