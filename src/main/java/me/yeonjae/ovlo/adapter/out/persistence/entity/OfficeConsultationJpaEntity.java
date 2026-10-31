package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "office_consultation",
        uniqueConstraints = @UniqueConstraint(name = "uq_office_consultation_room", columnNames = "chat_room_id"),
        indexes = {
                @Index(name = "idx_office_consultation_office_status", columnList = "office_id, status, opened_at"),
                @Index(name = "idx_office_consultation_member", columnList = "member_id, opened_at"),
                @Index(name = "idx_office_consultation_resolved", columnList = "status, resolved_at")
        })
public class OfficeConsultationJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "office_id", nullable = false) private Long officeId;
    @Column(name = "member_id", nullable = false) private Long memberId;
    @Column(name = "inquiry_id") private Long inquiryId;
    @Column(name = "consent_version", nullable = false, length = 10) private String consentVersion;
    @Column(name = "consent_text", nullable = false, columnDefinition = "TEXT") private String consentText;
    @Column(name = "shared_question", nullable = false, length = 1000) private String sharedQuestion;
    @Column(name = "shared_answer", columnDefinition = "TEXT") private String sharedAnswer;
    @Column(name = "shared_lang", nullable = false, length = 5) private String sharedLang;
    @Column(name = "shared_verified_student", nullable = false) private boolean sharedVerifiedStudent;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "assigned_staff_member_id") private Long assignedStaffMemberId;
    @Column(name = "chat_room_id", nullable = false) private Long chatRoomId;
    @Column(name = "opened_at", nullable = false) private Instant openedAt;
    @Column(name = "first_staff_reply_at") private Instant firstStaffReplyAt;
    @Column(name = "resolved_at") private Instant resolvedAt;
    @Column(name = "closed_at") private Instant closedAt;

    public OfficeConsultationJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOfficeId() { return officeId; }
    public void setOfficeId(Long officeId) { this.officeId = officeId; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public Long getInquiryId() { return inquiryId; }
    public void setInquiryId(Long inquiryId) { this.inquiryId = inquiryId; }
    public String getConsentVersion() { return consentVersion; }
    public void setConsentVersion(String consentVersion) { this.consentVersion = consentVersion; }
    public String getConsentText() { return consentText; }
    public void setConsentText(String consentText) { this.consentText = consentText; }
    public String getSharedQuestion() { return sharedQuestion; }
    public void setSharedQuestion(String sharedQuestion) { this.sharedQuestion = sharedQuestion; }
    public String getSharedAnswer() { return sharedAnswer; }
    public void setSharedAnswer(String sharedAnswer) { this.sharedAnswer = sharedAnswer; }
    public String getSharedLang() { return sharedLang; }
    public void setSharedLang(String sharedLang) { this.sharedLang = sharedLang; }
    public boolean isSharedVerifiedStudent() { return sharedVerifiedStudent; }
    public void setSharedVerifiedStudent(boolean sharedVerifiedStudent) { this.sharedVerifiedStudent = sharedVerifiedStudent; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getAssignedStaffMemberId() { return assignedStaffMemberId; }
    public void setAssignedStaffMemberId(Long assignedStaffMemberId) { this.assignedStaffMemberId = assignedStaffMemberId; }
    public Long getChatRoomId() { return chatRoomId; }
    public void setChatRoomId(Long chatRoomId) { this.chatRoomId = chatRoomId; }
    public Instant getOpenedAt() { return openedAt; }
    public void setOpenedAt(Instant openedAt) { this.openedAt = openedAt; }
    public Instant getFirstStaffReplyAt() { return firstStaffReplyAt; }
    public void setFirstStaffReplyAt(Instant firstStaffReplyAt) { this.firstStaffReplyAt = firstStaffReplyAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
}
