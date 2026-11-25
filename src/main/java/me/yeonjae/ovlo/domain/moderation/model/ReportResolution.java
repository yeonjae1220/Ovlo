package me.yeonjae.ovlo.domain.moderation.model;

/** 관리자가 신고를 처리하는 방법. */
public enum ReportResolution {
    DISMISS,
    REMOVE_CONTENT,
    SUSPEND_AUTHOR;

    public ReportStatus resultingStatus() {
        return this == DISMISS ? ReportStatus.DISMISSED : ReportStatus.ACTIONED;
    }

    /** 회원 신고에는 내릴 콘텐츠가 없다. */
    public boolean appliesTo(ReportTargetType type) {
        return this != REMOVE_CONTENT || type != ReportTargetType.MEMBER;
    }
}
