package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "university_report_translation")
public class UniversityReportTranslationJpaEntity {

    @EmbeddedId
    private TranslationId id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UniversityReportTranslationJpaEntity() {}

    @Embeddable
    public static class TranslationId implements Serializable {
        @Column(name = "report_id", nullable = false)
        private Long reportId;

        @Column(name = "lang", nullable = false, length = 5)
        private String lang;

        public TranslationId() {}
        public TranslationId(Long reportId, String lang) { this.reportId = reportId; this.lang = lang; }

        public Long getReportId() { return reportId; }
        public String getLang() { return lang; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TranslationId)) return false;
            TranslationId that = (TranslationId) o;
            return Objects.equals(reportId, that.reportId) && Objects.equals(lang, that.lang);
        }
        @Override public int hashCode() { return Objects.hash(reportId, lang); }
    }

    public TranslationId getId() { return id; }
    public void setId(TranslationId id) { this.id = id; }
    public Long getReportId() { return id != null ? id.getReportId() : null; }
    public String getLang() { return id != null ? id.getLang() : null; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
