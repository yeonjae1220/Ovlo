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

    private VerificationCredential(VerificationId id, Long memberId, VerificationType type,
                                   Long universityId, String verifiedEmail,
                                   VerificationStatus status, Instant verifiedAt) {
        this.id = id;
        this.memberId = memberId;
        this.type = type;
        this.universityId = universityId;
        this.verifiedEmail = verifiedEmail;
        this.status = status;
        this.verifiedAt = verifiedAt;
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
                VerificationStatus.VERIFIED, verifiedAt);
    }

    public static VerificationCredential restore(VerificationId id, Long memberId, VerificationType type,
                                                 Long universityId, String verifiedEmail,
                                                 VerificationStatus status, Instant verifiedAt) {
        return new VerificationCredential(id, memberId, type, universityId, verifiedEmail, status, verifiedAt);
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
}
