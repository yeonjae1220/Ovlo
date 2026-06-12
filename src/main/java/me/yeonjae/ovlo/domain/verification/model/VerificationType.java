package me.yeonjae.ovlo.domain.verification.model;

/**
 * 인증 수단.
 * MVP는 학교 이메일 인증(SCHOOL_EMAIL)만. 대상 대학은 자격에 별도로 귀속되며
 * 본교/파견 여부와 무관하게 "해당 대학의 학생 이메일을 소유함"을 의미한다.
 * (HOST_EMAIL=교환 상태 자동 증명, DOCUMENT=서류 fallback 은 차기)
 */
public enum VerificationType {
    SCHOOL_EMAIL
}
