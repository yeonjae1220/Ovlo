package me.yeonjae.ovlo.domain.verification.model;

/**
 * 발급된 인증 자격의 상태.
 * 이메일 인증은 코드 확인 시점에 곧바로 VERIFIED로 발급된다.
 * EXPIRED는 만료(예: 교환 학기 종료) 처리용 — MVP는 발급만, 만료 스케줄러는 차기.
 */
public enum VerificationStatus {
    VERIFIED,
    EXPIRED
}
