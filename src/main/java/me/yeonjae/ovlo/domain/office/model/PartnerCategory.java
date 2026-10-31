package me.yeonjae.ovlo.domain.office.model;

/** 입국 직후 필수 정착 제휴 분류(설계 §4.6). 노출 순서 가설: 유심 → 보험 → 숙소 → 픽업. */
public enum PartnerCategory {
    SIM, INSURANCE, HOUSING, PICKUP
}
