package me.yeonjae.ovlo.domain.verification.model;

import java.time.Instant;
import java.util.Objects;

/**
 * 발급된 인증 자격 (Aggregate Root).
 * "멤버가 특정 대학의 학생 이메일을 소유함"을 증명한다. 대상 대학은 본교/파견 무관.
 */
public class VerificationCredential {

    private VerificationId id;
    private final Long memberId;
    private final VerificationType type;
    private final Long universityId;
    private final String verifiedEmail;
    private VerificationStatus status;
    private final Instant verifiedAt;
    /** 수동 인증을 발급한 관리자 식별자(이메일). 셀프서비스 자격은 null. */
    private final String verifiedBy;
    /** 수동 인증 사유 메모. 셀프서비스 자격은 null. */
    private final String note;
    /** 관리자가 취소한 경우 취소자 식별자. 미취소/시스템 만료는 null. */
    private String revokedBy;
    /** 관리자가 취소한 시각. 미취소/시스템 만료는 null. */
    private Instant revokedAt;

    private VerificationCredential(VerificationId id, Long memberId, VerificationType type,
                                   Long universityId, String verifiedEmail,
                                   VerificationStatus status, Instant verifiedAt,
                                   String verifiedBy, String note,
                                   String revokedBy, Instant revokedAt) {
        this.id = id;
        this.memberId = memberId;
        this.type = type;
        this.universityId = universityId;
        this.verifiedEmail = verifiedEmail;
        this.status = status;
        this.verifiedAt = verifiedAt;
        this.verifiedBy = verifiedBy;
        this.note = note;
        this.revokedBy = revokedBy;
        this.revokedAt = revokedAt;
    }

    /** 코드 확인 성공 시 VERIFIED 상태로 발급. */
    public static VerificationCredential issue(Long memberId, VerificationType type,
                                               Long universityId, String verifiedEmail, Instant verifiedAt) {
        Objects.requireNonNull(memberId, "memberId는 필수입니다");
        Objects.requireNonNull(type, "type은 필수입니다");
        Objects.requireNonNull(universityId, "universityId는 필수입니다");
        if (verifiedEmail == null || verifiedEmail.isBlank()) {
            throw new IllegalArgumentException("인증된 이메일은 비어 있을 수 없습니다");
        }
        Objects.requireNonNull(verifiedAt, "verifiedAt은 필수입니다");
        return new VerificationCredential(null, memberId, type, universityId, verifiedEmail,
                VerificationStatus.VERIFIED, verifiedAt, null, null, null, null);
    }

    /**
     * 관리자 수동 인증 발급(ADMIN_VERIFIED). 학교 이메일 없이도 발급 가능하며
     * 발급자(verifiedBy)와 사유(note)를 감사용으로 보존한다.
     */
    public static VerificationCredential issueManual(Long memberId, Long universityId,
                                                     String verifiedEmail, String verifiedBy,
                                                     String note, Instant verifiedAt) {
        Objects.requireNonNull(memberId, "memberId는 필수입니다");
        Objects.requireNonNull(universityId, "universityId는 필수입니다");
        if (verifiedBy == null || verifiedBy.isBlank()) {
            throw new IllegalArgumentException("발급 관리자(verifiedBy)는 필수입니다");
        }
        Objects.requireNonNull(verifiedAt, "verifiedAt은 필수입니다");
        String email = (verifiedEmail == null || verifiedEmail.isBlank()) ? null : verifiedEmail;
        return new VerificationCredential(null, memberId, VerificationType.ADMIN_VERIFIED, universityId,
                email, VerificationStatus.VERIFIED, verifiedAt, verifiedBy, note, null, null);
    }

    public static VerificationCredential restore(VerificationId id, Long memberId, VerificationType type,
                                                 Long universityId, String verifiedEmail,
                                                 VerificationStatus status, Instant verifiedAt,
                                                 String verifiedBy, String note,
                                                 String revokedBy, Instant revokedAt) {
        return new VerificationCredential(id, memberId, type, universityId, verifiedEmail, status, verifiedAt,
                verifiedBy, note, revokedBy, revokedAt);
    }

    public void assignId(VerificationId id) {
        this.id = id;
    }

    public void expire() {
        this.status = VerificationStatus.EXPIRED;
    }

    public boolean isActive() {
        return status == VerificationStatus.VERIFIED;
    }

    public VerificationId getId()        { return id; }
    public Long getMemberId()            { return memberId; }
    public VerificationType getType()    { return type; }
    public Long getUniversityId()        { return universityId; }
    public String getVerifiedEmail()     { return verifiedEmail; }
    public VerificationStatus getStatus(){ return status; }
    public Instant getVerifiedAt()       { return verifiedAt; }
    public String getVerifiedBy()        { return verifiedBy; }
    public String getNote()              { return note; }
    public String getRevokedBy()         { return revokedBy; }
    public Instant getRevokedAt()        { return revokedAt; }
}
