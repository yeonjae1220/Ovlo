package me.yeonjae.ovlo.domain.auth.model;

/**
 * 제출된 refresh 토큰으로 세션 교체를 시도한 결과.
 * 저장소가 "제출 토큰 == 현재 저장 토큰" 비교와 교체를 한 원자 구간에서 판정한다.
 */
public enum RefreshRotationOutcome {
    /** 제출 토큰이 현재 토큰 — 교체 성공. 같은 토큰으로 시작한 요청 중 정확히 하나만 받는다. */
    ROTATED,
    /** 제출 토큰이 직전 토큰이고 교체 직후 유예 시간 이내 — 동시 재발급 경합에서 진 요청. */
    CONCURRENT,
    /** 제출 토큰이 직전 토큰이지만 유예 시간이 지남 — 이미 교체된 토큰의 재사용. */
    REUSED,
    /** 세션 없음·만료·무효화 또는 알 수 없는 토큰. */
    INVALID
}
