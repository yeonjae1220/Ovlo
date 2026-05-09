package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "university_report_translation")
public class UniversityReportTranslationJpaEntity {

    @Embeddable
    public static class TranslationId implements Serializable {
        @Column(name = "report_id") private Long reportId;
        @Column(name = "lang")      private String lang;

        public TranslationId() {}
        public TranslationId(Long reportId, String lang) { this.reportId = reportId; this.lang = lang; }

        public Long getReportId() { return reportId; }
        public String getLang()   { return lang; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TranslationId that)) return false;
            return Objects.equals(reportId, that.reportId) && Objects.equals(lang, that.lang);
        }
        @Override public int hashCode() { return Objects.hash(reportId, lang); }
    }

    @EmbeddedId
    private TranslationId id;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UniversityReportTranslationJpaEntity() {}

    public TranslationId getId()    { return id; }
    public Long getReportId()       { return id != null ? id.getReportId() : null; }
    public String getLang()         { return id != null ? id.getLang() : null; }
    public String getTitle()        { return title; }
    public String getSummary()      { return summary; }
    public String getBody()         { return body; }
    public String getContent()      { return content; }
}
