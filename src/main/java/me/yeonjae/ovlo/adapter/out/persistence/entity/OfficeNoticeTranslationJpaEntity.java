package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "office_notice_translation",
        uniqueConstraints = @UniqueConstraint(name = "uq_office_notice_translation", columnNames = {"notice_id", "lang"}))
public class OfficeNoticeTranslationJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notice_id", nullable = false)
    private OfficeNoticeJpaEntity notice;

    @Column(nullable = false, length = 5)
    private String lang;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "source_version", nullable = false)
    private int sourceVersion;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    public OfficeNoticeTranslationJpaEntity() {}

    public Long getId() { return id; }
    public OfficeNoticeJpaEntity getNotice() { return notice; }
    public void setNotice(OfficeNoticeJpaEntity notice) { this.notice = notice; }
    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = lang; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getSourceVersion() { return sourceVersion; }
    public void setSourceVersion(int sourceVersion) { this.sourceVersion = sourceVersion; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
