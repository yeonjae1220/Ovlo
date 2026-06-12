package me.yeonjae.ovlo.domain.verification.model;

import java.util.Collection;

/**
 * 멤버의 신뢰 등급 — 보유 자격에서 파생되는 값(저장하지 않음).
 *
 * <ul>
 *   <li>{@code UNVERIFIED} : 인증 없음</li>
 *   <li>{@code STUDENT}    : 학교 이메일 인증 1건 이상 ("학생 인증")</li>
 * </ul>
 *
 * 교환 상태까지 증명하는 {@code EXCHANGE_VERIFIED}(호스트 이메일)는 차기.
 */
public enum TrustLevel {
    UNVERIFIED,
    STUDENT;

    /** 보유 자격 집합에서 신뢰 등급을 파생한다. */
    public static TrustLevel from(Collection<VerificationCredential> credentials) {
        if (credentials == null) {
            return UNVERIFIED;
        }
        boolean hasVerifiedSchoolEmail = credentials.stream()
                .anyMatch(c -> c.isActive() && c.getType() == VerificationType.SCHOOL_EMAIL);
        return hasVerifiedSchoolEmail ? STUDENT : UNVERIFIED;
    }
}
