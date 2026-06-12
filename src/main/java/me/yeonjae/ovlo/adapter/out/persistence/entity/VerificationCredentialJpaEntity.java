package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "verification_credential",
        uniqueConstraints = @UniqueConstraint(name = "uq_vc_member_type", columnNames = {"member_id", "type"}))
public class VerificationCredentialJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(name = "university_id", nullable = false)
    private Long universityId;

    @Column(name = "verified_email", nullable = false)
    private String verifiedEmail;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public VerificationCredentialJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getUniversityId() { return universityId; }
    public void setUniversityId(Long universityId) { this.universityId = universityId; }
    public String getVerifiedEmail() { return verifiedEmail; }
    public void setVerifiedEmail(String verifiedEmail) { this.verifiedEmail = verifiedEmail; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
}
