package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.dto.result.ReportSummaryResult;
import me.yeonjae.ovlo.application.port.out.moderation.ContentReportPort;
import me.yeonjae.ovlo.domain.moderation.model.ContentReport;
import me.yeonjae.ovlo.domain.moderation.model.ReportReason;
import me.yeonjae.ovlo.domain.moderation.model.ReportResolution;
import me.yeonjae.ovlo.domain.moderation.model.ReportStatus;
import me.yeonjae.ovlo.domain.moderation.model.ReportTargetType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ContentReportPersistenceAdapter implements ContentReportPort {

    private static final int PREVIEW_LENGTH = 200;

    private final NamedParameterJdbcTemplate jdbc;

    public ContentReportPersistenceAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean save(ContentReport report) {
        var params = new MapSqlParameterSource()
                .addValue("type", report.targetType().name())
                .addValue("target", report.targetId())
                .addValue("author", report.targetAuthorId())
                .addValue("reporter", report.reporterId())
                .addValue("reason", report.reason().name())
                .addValue("detail", report.detail());
        // 처리 대기 중인 재신고는 첫 신고를 유지한다(처리된 뒤에는 새로 접수된다). 예외를 잡아 넘기면 PostgreSQL 트랜잭션이 깨지므로 ON CONFLICT 로 처리한다.
        return jdbc.update("""
                INSERT INTO content_report(target_type, target_id, target_author_id, reporter_id, reason, detail)
                VALUES(:type, :target, :author, :reporter, :reason, :detail)
                ON CONFLICT (target_type, target_id, reporter_id) WHERE status = 'OPEN' DO NOTHING
                """, params) > 0;
    }

    @Override
    public List<ReportSummaryResult> findByStatus(ReportStatus status, int limit) {
        return jdbc.query("""
                SELECT r.id, r.target_type, r.target_id, r.target_author_id, author.nickname, reporter.nickname,
                       r.reason, r.detail, r.status, r.resolution, r.created_at,
                       CASE r.target_type
                           WHEN 'POST' THEN (SELECT p.title || ' — ' || p.content FROM post p WHERE p.id = r.target_id)
                           WHEN 'COMMENT' THEN (SELECT c.content FROM comment c WHERE c.id = r.target_id)
                           WHEN 'CHAT_MESSAGE' THEN (SELECT msg.content FROM message msg WHERE msg.id = r.target_id)
                       END AS preview,
                       (SELECT count(*) FROM content_report o
                        WHERE o.target_type = r.target_type AND o.target_id = r.target_id AND o.status = 'OPEN') AS open_count
                FROM content_report r
                JOIN member author ON author.id = r.target_author_id
                JOIN member reporter ON reporter.id = r.reporter_id
                WHERE r.status = :status
                ORDER BY r.id DESC
                LIMIT :limit
                """, Map.of("status", status.name(), "limit", limit), (rs, n) -> new ReportSummaryResult(
                rs.getLong("id"),
                ReportTargetType.valueOf(rs.getString("target_type")),
                rs.getLong("target_id"),
                rs.getLong("target_author_id"),
                rs.getString(5),
                rs.getString(6),
                ReportReason.valueOf(rs.getString("reason")),
                rs.getString("detail"),
                ReportStatus.valueOf(rs.getString("status")),
                rs.getString("resolution"),
                rs.getTimestamp("created_at").toInstant(),
                truncate(rs.getString("preview")),
                rs.getLong("open_count")));
    }

    private static String truncate(String text) {
        if (text == null || text.length() <= PREVIEW_LENGTH) return text;
        return text.substring(0, PREVIEW_LENGTH) + "…";
    }

    @Override
    public Optional<ReportTarget> findTarget(long reportId) {
        return jdbc.query("SELECT target_type, target_id, target_author_id, status FROM content_report WHERE id = :id",
                Map.of("id", reportId), (rs, n) -> new ReportTarget(
                        ReportTargetType.valueOf(rs.getString(1)), rs.getLong(2), rs.getLong(3),
                        ReportStatus.valueOf(rs.getString(4)))).stream().findFirst();
    }

    @Override
    public int resolveOpenReports(ReportTargetType type, long targetId, ReportResolution resolution, String adminName) {
        return jdbc.update("""
                UPDATE content_report
                SET status = :status, resolution = :resolution, resolved_by = :admin, resolved_at = CURRENT_TIMESTAMP
                WHERE target_type = :type AND target_id = :target AND status = 'OPEN'
                """, new MapSqlParameterSource()
                .addValue("status", resolution.resultingStatus().name())
                .addValue("resolution", resolution.name())
                .addValue("admin", adminName)
                .addValue("type", type.name())
                .addValue("target", targetId));
    }
}
