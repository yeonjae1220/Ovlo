package me.yeonjae.ovlo.domain.moderation.model;

import me.yeonjae.ovlo.domain.moderation.exception.ModerationException;

/**
 * 새로 접수되는 신고. 대상 작성자는 신고 시점에 고정해 둔다.
 */
public record ContentReport(
        ReportTargetType targetType,
        long targetId,
        long targetAuthorId,
        long reporterId,
        ReportReason reason,
        String detail
) {
    public static final int MAX_DETAIL_LENGTH = 1000;

    public ContentReport {
        if (targetType == null || reason == null) {
            throw new ModerationException("신고 대상과 사유가 필요합니다", ModerationException.ErrorType.BAD_REQUEST);
        }
        if (targetId <= 0 || targetAuthorId <= 0 || reporterId <= 0) {
            throw new ModerationException("올바르지 않은 신고 대상입니다", ModerationException.ErrorType.BAD_REQUEST);
        }
        if (reporterId == targetAuthorId) {
            throw new ModerationException("자신의 콘텐츠는 신고할 수 없습니다", ModerationException.ErrorType.BAD_REQUEST);
        }
        detail = detail == null || detail.isBlank() ? null : detail.strip();
        if (detail != null && detail.length() > MAX_DETAIL_LENGTH) {
            throw new ModerationException("상세 내용은 1000자 이하여야 합니다", ModerationException.ErrorType.BAD_REQUEST);
        }
        if (reason == ReportReason.OTHER && detail == null) {
            throw new ModerationException("기타 사유는 상세 내용을 적어야 합니다", ModerationException.ErrorType.BAD_REQUEST);
        }
    }
}
