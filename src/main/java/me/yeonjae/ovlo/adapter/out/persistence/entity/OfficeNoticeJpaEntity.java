package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "office_notice",
        indexes = {
                @Index(name = "idx_office_notice_office_status", columnList = "office_id, status, expires_at"),
                @Index(name = "idx_office_notice_status_expires", columnList = "status, expires_at")
        })
public class OfficeNoticeJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "office_id", nullable = false)
    private Long officeId;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "source_lang", nullable = false, length = 5)
    private String sourceLang;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    /** 쉼표 구분 media id 목록. 비어 있으면 null. */
    @Column(name = "attachment_media_ids", length = 1000)
    private String attachmentMediaIds;

    @Column(nullable = false, length = 20)
    private String term;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "expires_at", nullable = false)
    private LocalDate expiresAt;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "approved_by", length = 255)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "expiry_warned_at")
    private Instant expiryWarnedAt;

    @Column(nullable = false)
    private int version;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OfficeNoticeTranslationJpaEntity> translations = new ArrayList<>();

    public OfficeNoticeJpaEntity() {}

    public void addTranslation(OfficeNoticeTranslationJpaEntity t) {
        t.setNotice(this);
        translations.add(t);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOfficeId() { return officeId; }
    public void setOfficeId(Long officeId) { this.officeId = officeId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getSourceLang() { return sourceLang; }
    public void setSourceLang(String sourceLang) { this.sourceLang = sourceLang; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getAttachmentMediaIds() { return attachmentMediaIds; }
    public void setAttachmentMediaIds(String attachmentMediaIds) { this.attachmentMediaIds = attachmentMediaIds; }
    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public Instant getExpiryWarnedAt() { return expiryWarnedAt; }
    public void setExpiryWarnedAt(Instant expiryWarnedAt) { this.expiryWarnedAt = expiryWarnedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<OfficeNoticeTranslationJpaEntity> getTranslations() { return translations; }
}
