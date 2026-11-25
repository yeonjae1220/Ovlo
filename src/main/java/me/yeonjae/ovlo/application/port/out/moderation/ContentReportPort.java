package me.yeonjae.ovlo.application.port.out.moderation;

import me.yeonjae.ovlo.application.dto.result.ReportSummaryResult;
import me.yeonjae.ovlo.domain.moderation.model.ContentReport;
import me.yeonjae.ovlo.domain.moderation.model.ReportResolution;
import me.yeonjae.ovlo.domain.moderation.model.ReportStatus;
import me.yeonjae.ovlo.domain.moderation.model.ReportTargetType;

import java.util.List;
import java.util.Optional;

public interface ContentReportPort {
    /** @return 새로 접수됐으면 true, 같은 신고자가 이미 신고한 대상이면 false */
    boolean save(ContentReport report);

    List<ReportSummaryResult> findByStatus(ReportStatus status, int limit);

    Optional<ReportTarget> findTarget(long reportId);

    /** 대상의 OPEN 신고를 모두 닫는다. @return 닫은 건수 */
    int resolveOpenReports(ReportTargetType type, long targetId, ReportResolution resolution, String adminName);

    record ReportTarget(ReportTargetType type, long targetId, long targetAuthorId, ReportStatus status) {}
}
