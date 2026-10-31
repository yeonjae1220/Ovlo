package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "office_inquiry_feedback")
public class OfficeInquiryFeedbackJpaEntity {

    @Id
    @Column(name = "inquiry_id")
    private Long inquiryId;

    @Column(nullable = false)
    private boolean helpful;

    @Column(length = 300)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public OfficeInquiryFeedbackJpaEntity() {}

    public Long getInquiryId() { return inquiryId; }
    public void setInquiryId(Long inquiryId) { this.inquiryId = inquiryId; }
    public boolean isHelpful() { return helpful; }
    public void setHelpful(boolean helpful) { this.helpful = helpful; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
