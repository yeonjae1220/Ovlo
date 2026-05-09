package me.yeonjae.ovlo.domain.university.model;

public class UniversityReportTranslation {

    private final Long reportId;
    private final String lang;
    private final String title;
    private final String summary;
    private final String body;
    private final String content;  // JSONB as raw JSON string

    private UniversityReportTranslation(Long reportId, String lang, String title,
                                        String summary, String body, String content) {
        this.reportId = reportId;
        this.lang = lang;
        this.title = title;
        this.summary = summary;
        this.body = body;
        this.content = content;
    }

    public static UniversityReportTranslation restore(Long reportId, String lang, String title,
                                                      String summary, String body, String content) {
        return new UniversityReportTranslation(reportId, lang, title, summary, body, content);
    }

    public Long getReportId() { return reportId; }
    public String getLang()   { return lang; }
    public String getTitle()  { return title; }
    public String getSummary(){ return summary; }
    public String getBody()   { return body; }
    public String getContent(){ return content; }
}
