package me.yeonjae.ovlo.application.port.in.moderation;

import me.yeonjae.ovlo.application.dto.result.ReportSummaryResult;
import me.yeonjae.ovlo.domain.moderation.model.ReportResolution;
import me.yeonjae.ovlo.domain.moderation.model.ReportStatus;

import java.util.List;

public interface ModerateReportUseCase {
    List<ReportSummaryResult> reports(ReportStatus status);

    /** 같은 대상에 쌓인 처리 대기 신고를 모두 함께 닫는다. 이미 처리된 신고면 CONFLICT. */
    void resolve(String adminName, long reportId, ReportResolution resolution);
}
