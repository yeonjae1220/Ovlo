package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "office_inquiry",
        indexes = {
                @Index(name = "idx_office_inquiry_office_asked", columnList = "office_id, asked_at"),
                @Index(name = "idx_office_inquiry_member", columnList = "member_id, asked_at")
        })
public class OfficeInquiryJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "office_id", nullable = false)
    private Long officeId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 5)
    private String lang;

    @Column(nullable = false, length = 1000)
    private String question;

    @Column(nullable = false, length = 20)
    private String outcome;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "cited_notice_ids", length = 500)
    private String citedNoticeIds;

    @Column(name = "candidate_notice_ids", length = 500)
    private String candidateNoticeIds;

    @Column(name = "asked_at", nullable = false)
    private Instant askedAt;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "model_id", length = 100)
    private String modelId;

    public OfficeInquiryJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOfficeId() { return officeId; }
    public void setOfficeId(Long officeId) { this.officeId = officeId; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = lang; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    public String getCitedNoticeIds() { return citedNoticeIds; }
    public void setCitedNoticeIds(String citedNoticeIds) { this.citedNoticeIds = citedNoticeIds; }
    public String getCandidateNoticeIds() { return candidateNoticeIds; }
    public void setCandidateNoticeIds(String candidateNoticeIds) { this.candidateNoticeIds = candidateNoticeIds; }
    public Instant getAskedAt() { return askedAt; }
    public void setAskedAt(Instant askedAt) { this.askedAt = askedAt; }
    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
}
