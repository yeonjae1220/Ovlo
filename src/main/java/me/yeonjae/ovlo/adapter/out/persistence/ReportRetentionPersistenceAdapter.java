package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.port.out.moderation.ReportRetentionPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

@Component
public class ReportRetentionPersistenceAdapter implements ReportRetentionPort {

    private final JdbcTemplate jdbc;

    public ReportRetentionPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public int anonymizeOlderThan(Instant cutoff) {
        var at = Timestamp.from(cutoff);
        // 처리 대기(OPEN) 신고는 resolved_at 이 없어 대상이 아니다 — 처리가 끝난 뒤부터 1년을 센다.
        int content = jdbc.update("""
                UPDATE content_report
                SET target_id = NULL, target_author_id = NULL, reporter_id = NULL, detail = NULL, resolved_by = NULL,
                    anonymized_at = CURRENT_TIMESTAMP
                WHERE anonymized_at IS NULL AND resolved_at IS NOT NULL AND resolved_at <= ?
                """, at);
        int marketplace = jdbc.update("""
                UPDATE marketplace_report
                SET listing_id = NULL, reporter_id = NULL, reason = NULL, anonymized_at = CURRENT_TIMESTAMP
                WHERE anonymized_at IS NULL AND created_at <= ?
                """, at);
        return content + marketplace;
    }
}
