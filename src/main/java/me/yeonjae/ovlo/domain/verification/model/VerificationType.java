package me.yeonjae.ovlo.domain.verification.model;

/**
 * 인증 수단.
 * <ul>
 *   <li>{@code SCHOOL_EMAIL}   : 학교 이메일 코드 인증(셀프서비스)</li>
 *   <li>{@code ADMIN_VERIFIED} : 관리자 수동 인증(증빙서류 등) — 학교 이메일 없이 발급 가능</li>
 * </ul>
 * 대상 대학은 자격에 별도로 귀속되며 본교/파견 여부와 무관하게
 * "해당 대학의 학생임"을 의미한다. (HOST_EMAIL=교환 상태 자동 증명은 차기)
 */
public enum VerificationType {
    SCHOOL_EMAIL,
    ADMIN_VERIFIED;

    /** 이 타입의 활성 자격이 학생 신뢰 등급을 부여하는지. */
    public boolean grantsStudentTrust() {
        return this == SCHOOL_EMAIL || this == ADMIN_VERIFIED;
    }
}
