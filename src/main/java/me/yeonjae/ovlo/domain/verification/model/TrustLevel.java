package me.yeonjae.ovlo.domain.verification.model;

import java.util.Collection;

/**
 * 멤버의 신뢰 등급 — 보유 자격에서 파생되는 값(저장하지 않음).
 *
 * <ul>
 *   <li>{@code UNVERIFIED}        : 인증 없음</li>
 *   <li>{@code STUDENT}           : 학교 이메일 인증 1건 이상 ("학생 인증")</li>
 *   <li>{@code EXCHANGE_VERIFIED} : 본교가 아닌 대학의 학교 이메일 인증 보유 ("교환 인증")
 *       — 다른 기관의 학생 이메일 소유를 증명 = 파견(교환) 상태로 간주</li>
 * </ul>
 *
 * 등급 순위: UNVERIFIED &lt; STUDENT &lt; EXCHANGE_VERIFIED.
 */
public enum TrustLevel {
    UNVERIFIED,
    STUDENT,
    EXCHANGE_VERIFIED;

    /** 이 등급이 {@code required} 이상인지(게시판 트러스트 게이팅 등에 사용). 순위는 선언 순서. */
    public boolean atLeast(TrustLevel required) {
        return this.ordinal() >= required.ordinal();
    }

    /** 문자열명을 TrustLevel로 파싱하되 알 수 없으면 UNVERIFIED. */
    public static TrustLevel parseOrUnverified(String name) {
        if (name == null || name.isBlank()) {
            return UNVERIFIED;
        }
        try {
            return TrustLevel.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNVERIFIED;
        }
    }

    /**
     * 본교 정보 없이 파생.
     * 본교와의 비교가 불가능하므로 EXCHANGE_VERIFIED로는 올라가지 않고 STUDENT까지만 판정한다.
     */
    public static TrustLevel from(Collection<VerificationCredential> credentials) {
        return from(credentials, null);
    }

    /**
     * 보유 자격 + 본교 대학 ID로 신뢰 등급을 파생한다.
     * 본교와 다른 대학의 활성 학교 이메일 인증이 있으면 EXCHANGE_VERIFIED,
     * 그 외 활성 학교 이메일 인증이 있으면 STUDENT, 없으면 UNVERIFIED.
     *
     * @param homeUniversityId 본교 대학 ID(없으면 null) — null이면 교환 여부 판정 불가
     */
    public static TrustLevel from(Collection<VerificationCredential> credentials, Long homeUniversityId) {
        if (credentials == null) {
            return UNVERIFIED;
        }
        boolean hasActiveSchoolEmail = false;
        boolean hasNonHomeSchoolEmail = false;
        for (VerificationCredential c : credentials) {
            if (c.isActive() && c.getType().grantsStudentTrust()) {
                hasActiveSchoolEmail = true;
                if (homeUniversityId != null && !homeUniversityId.equals(c.getUniversityId())) {
                    hasNonHomeSchoolEmail = true;
                }
            }
        }
        if (hasNonHomeSchoolEmail) {
            return EXCHANGE_VERIFIED;
        }
        return hasActiveSchoolEmail ? STUDENT : UNVERIFIED;
    }
}
