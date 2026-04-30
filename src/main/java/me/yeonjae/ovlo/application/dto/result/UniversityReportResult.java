package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.report.model.UniversityReport;
import me.yeonjae.ovlo.domain.report.model.UniversityReportTranslation;

import java.time.Instant;
import java.util.List;

public record UniversityReportResult(
        Long id,
        Long globalUnivId,
        Long exchangeUnivId,
        String lang,
        String title,
        String summary,
        String body,
        String content,
        int sourceVideoCount,
        int sourceWebCount,
        String costCurrency,
        List<String> supportedLangs,
        Instant createdAt
) {
    public static UniversityReportResult from(UniversityReport report, UniversityReportTranslation translation) {
        return new UniversityReportResult(
                report.getId(),
                report.getGlobalUnivId(),
                report.getExchangeUnivId(),
                translation.getLang(),
                translation.getTitle(),
                translation.getSummary(),
                translation.getBody(),
                translation.getContent(),
                report.getSourceVideoCount(),
                report.getSourceWebCount(),
                report.getCostCurrency(),
                report.getSupportedLangs(),
                report.getCreatedAt()
        );
    }

    public static UniversityReportResult summary(UniversityReport report, UniversityReportTranslation translation) {
        return new UniversityReportResult(
                report.getId(),
                report.getGlobalUnivId(),
                report.getExchangeUnivId(),
                translation.getLang(),
                translation.getTitle(),
                translation.getSummary(),
                null,
                null,
                report.getSourceVideoCount(),
                report.getSourceWebCount(),
                report.getCostCurrency(),
                report.getSupportedLangs(),
                report.getCreatedAt()
        );
    }
}
