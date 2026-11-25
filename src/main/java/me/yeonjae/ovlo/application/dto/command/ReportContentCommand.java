package me.yeonjae.ovlo.application.dto.command;

import me.yeonjae.ovlo.domain.moderation.model.ReportReason;
import me.yeonjae.ovlo.domain.moderation.model.ReportTargetType;

public record ReportContentCommand(
        long reporterId,
        ReportTargetType targetType,
        long targetId,
        ReportReason reason,
        String detail
) {}
