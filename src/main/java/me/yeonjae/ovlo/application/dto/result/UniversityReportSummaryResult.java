package me.yeonjae.ovlo.application.dto.result;

import java.util.List;

public record UniversityReportSummaryResult(
        Long id,
        String title,
        String summary,
        int sourceVideoCount,
        int sourceWebCount,
        List<String> supportedLangs
) {}
