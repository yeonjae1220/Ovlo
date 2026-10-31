package me.yeonjae.ovlo.domain.office.model;

/** OPEN(접수) → IN_PROGRESS(담당자 배정) → RESOLVED(담당자 해결) → CLOSED(학생 확인 또는 7일 경과). CLOSED 는 재개 불가. */
public enum ConsultationStatus {
    OPEN, IN_PROGRESS, RESOLVED, CLOSED
}
