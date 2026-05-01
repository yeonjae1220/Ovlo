package me.yeonjae.ovlo.application.dto.result;

public record UniversityReportResult(
        Long id,
        Long globalUnivId,
        String lang,
        String title,
        String summary,
        String body,
        String content,     // raw JSON string (JSONB)
        int sourceVideoCount,
        int sourceWebCount
) {}
