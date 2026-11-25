package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.moderation.model.ReportReason;
import me.yeonjae.ovlo.domain.moderation.model.ReportStatus;
import me.yeonjae.ovlo.domain.moderation.model.ReportTargetType;

import java.time.Instant;

/** 관리자 신고 목록 한 줄. contentPreview 는 대상이 이미 지워졌으면 null 이다. */
public record ReportSummaryResult(
        long id,
        ReportTargetType targetType,
        long targetId,
        long targetAuthorId,
        String targetAuthorNickname,
        String reporterNickname,
        ReportReason reason,
        String detail,
        ReportStatus status,
        String resolution,
        Instant createdAt,
        String contentPreview,
        long openReportsOnTarget
) {}
