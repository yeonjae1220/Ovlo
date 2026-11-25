package me.yeonjae.ovlo.domain.moderation.model;

public enum ReportStatus {
    OPEN,
    /** 관리자가 콘텐츠를 내리거나 작성자를 정지했다. */
    ACTIONED,
    /** 관리자가 검토 후 조치하지 않았다. */
    DISMISSED
}
