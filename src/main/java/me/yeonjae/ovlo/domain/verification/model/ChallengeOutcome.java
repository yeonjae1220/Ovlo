package me.yeonjae.ovlo.domain.verification.model;

/** 이메일 인증 코드 확인 결과. */
public enum ChallengeOutcome {
    SUCCESS,
    MISMATCH,
    EXPIRED,
    EXHAUSTED
}
