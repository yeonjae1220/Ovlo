package me.yeonjae.ovlo.domain.report.model;

import java.time.Instant;

public class UniversityReportTranslation {
    private final Long reportId;
    private final String lang;
    private final String title;
    private final String summary;
    private final String body;
    private final String content;
    private final Instant createdAt;
    private final Instant updatedAt;

    public UniversityReportTranslation(Long reportId, String lang, String title,
                                       String summary, String body, String content,
                                       Instant createdAt, Instant updatedAt) {
        this.reportId = reportId;
        this.lang = lang;
        this.title = title;
        this.summary = summary;
        this.body = body;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getReportId() { return reportId; }
    public String getLang() { return lang; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getBody() { return body; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
