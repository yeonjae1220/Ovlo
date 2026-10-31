package me.yeonjae.ovlo.domain.office.model;

/** DRAFT → APPROVED(승인자·만료일 필수) → EXPIRED(만료일 경과, 시스템) → (새 만료일 후) APPROVED. 어디서든 ARCHIVED. */
public enum NoticeStatus {
    DRAFT, APPROVED, EXPIRED, ARCHIVED
}
