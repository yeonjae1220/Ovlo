package me.yeonjae.ovlo.domain.verification.model;

import java.time.Instant;
import java.util.Objects;

/**
 * 발급된 이메일 인증 챌린지 (Redis에 TTL과 함께 보관).
 * 대상 멤버·대학·학교이메일에 묶인 6자리 코드와 만료·시도횟수를 관리한다.
 * 보안: 짧은 TTL + 시도횟수 캡(코드 무차별 대입 차단). 코드는 해시하지 않는다(엔트로피 부족).
 */
public class EmailVerificationChallenge {

    private final Long memberId;
    private final Long universityId;
    private final String targetEmail;
    private final VerificationCode code;
    private final Instant expiresAt;
    private final int maxAttempts;
    private int attemptCount;

    private EmailVerificationChallenge(Long memberId, Long universityId, String targetEmail,
                                       VerificationCode code, Instant expiresAt,
                                       int maxAttempts, int attemptCount) {
        this.memberId = memberId;
        this.universityId = universityId;
        this.targetEmail = targetEmail;
        this.code = code;
        this.expiresAt = expiresAt;
        this.maxAttempts = maxAttempts;
        this.attemptCount = attemptCount;
    }

    public static EmailVerificationChallenge create(Long memberId, Long universityId, String targetEmail,
                                                    VerificationCode code, Instant expiresAt, int maxAttempts) {
        Objects.requireNonNull(memberId, "memberId는 필수입니다");
        Objects.requireNonNull(universityId, "universityId는 필수입니다");
        if (targetEmail == null || targetEmail.isBlank()) {
            throw new IllegalArgumentException("대상 이메일은 비어 있을 수 없습니다");
        }
        Objects.requireNonNull(code, "코드는 필수입니다");
        Objects.requireNonNull(expiresAt, "만료 시각은 필수입니다");
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("최대 시도횟수는 1 이상이어야 합니다");
        }
        return new EmailVerificationChallenge(memberId, universityId, targetEmail, code, expiresAt, maxAttempts, 0);
    }

    /** Redis 등 저장소에서 복원 (시도횟수 포함). */
    public static EmailVerificationChallenge restore(Long memberId, Long universityId, String targetEmail,
                                                     VerificationCode code, Instant expiresAt,
                                                     int maxAttempts, int attemptCount) {
        return new EmailVerificationChallenge(memberId, universityId, targetEmail, code, expiresAt, maxAttempts, attemptCount);
    }

    /**
     * 입력 코드를 검증한다. 만료·시도소진은 코드 비교 없이 차단하고,
     * 그 외에는 시도횟수를 1 소비한 뒤 일치 여부를 돌려준다.
     */
    public ChallengeOutcome verify(VerificationCode input, Instant now) {
        if (isExpired(now)) {
            return ChallengeOutcome.EXPIRED;
        }
        if (attemptCount >= maxAttempts) {
            return ChallengeOutcome.EXHAUSTED;
        }
        attemptCount++;
        return code.matches(input) ? ChallengeOutcome.SUCCESS : ChallengeOutcome.MISMATCH;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt); // now >= expiresAt
    }

    public int remainingAttempts() {
        return Math.max(0, maxAttempts - attemptCount);
    }

    public Long getMemberId()      { return memberId; }
    public Long getUniversityId()  { return universityId; }
    public String getTargetEmail() { return targetEmail; }
    public VerificationCode getCode() { return code; }
    public Instant getExpiresAt()  { return expiresAt; }
    public int getMaxAttempts()    { return maxAttempts; }
    public int getAttemptCount()   { return attemptCount; }
}
