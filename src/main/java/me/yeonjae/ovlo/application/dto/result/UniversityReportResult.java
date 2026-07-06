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
        int sourceWebCount,
        String countryCode  // 대학 소재국 ISO-3166 alpha-2 (디스플레이 통화 환산용)
) {}
