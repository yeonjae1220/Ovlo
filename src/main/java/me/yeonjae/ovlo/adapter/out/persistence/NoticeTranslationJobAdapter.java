package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.port.out.office.NoticeTranslationJobPort;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** PostgreSQL time and owner fencing coordinate workers across restarts and app instances. */
@Component
public class NoticeTranslationJobAdapter implements NoticeTranslationJobPort {
    private final JdbcTemplate jdbc;
    public NoticeTranslationJobAdapter(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @Override @Transactional
    public void enqueue(long id, int version, List<NoticeLang> languages) {
        for (var lang : languages) jdbc.update("""
                INSERT INTO notice_translation_job(notice_id, source_version, lang) VALUES (?,?,?)
                ON CONFLICT DO NOTHING
                """, id, version, lang.code());
    }

    @Override public Optional<Claim> claim(long id, int version, NoticeLang lang) {
        var owner=UUID.randomUUID();
        int changed=jdbc.update("""
                UPDATE notice_translation_job SET state='RUNNING',owner=?,lease_until=CURRENT_TIMESTAMP+INTERVAL '300 seconds'
                WHERE notice_id=? AND source_version=? AND lang=? AND attempts<3 AND created_at>CURRENT_TIMESTAMP-INTERVAL '48 hours'
                AND ((state='PENDING' AND available_at<=CURRENT_TIMESTAMP) OR (state='RUNNING' AND lease_until<CURRENT_TIMESTAMP))
                """, owner,id,version,lang.code());
        return changed==1 ? Optional.of(new Claim(id,version,lang,owner)) : Optional.empty();
    }

    @Override public void activateApproved(long id, int version) {
        // A draft may have exhausted its queue lifetime before staff approves it; retain paid attempt counts.
        jdbc.update("""
                UPDATE notice_translation_job SET state='PENDING',created_at=CURRENT_TIMESTAMP,available_at=CURRENT_TIMESTAMP
                WHERE notice_id=? AND source_version=? AND state IN ('PENDING','CANCELLED') AND attempts<3
                """,id,version);
    }

    @Override public boolean isHeld(Claim c) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM notice_translation_job WHERE notice_id=? AND source_version=? AND lang=?
                AND owner=? AND state='RUNNING' AND lease_until>CURRENT_TIMESTAMP FOR UPDATE)
                """, Boolean.class,c.noticeId(),c.version(),c.lang().code(),c.owner()));
    }

    @Override public void finish(Claim c, boolean rejected) {
        jdbc.update("""
                UPDATE notice_translation_job SET state=?,owner=NULL,lease_until=NULL,available_at=CURRENT_TIMESTAMP+INTERVAL '60 seconds'
                WHERE notice_id=? AND source_version=? AND lang=? AND owner=? AND state='RUNNING' AND lease_until>CURRENT_TIMESTAMP
                """, rejected?"REJECTED":"DONE",c.noticeId(),c.version(),c.lang().code(),c.owner());
    }

    @Override public void reopenRejected(long id, int version) {
        jdbc.update("""
                UPDATE notice_translation_job SET state='PENDING' WHERE notice_id=? AND source_version=?
                AND state='REJECTED' AND attempts<3 AND available_at<=CURRENT_TIMESTAMP
                AND created_at>CURRENT_TIMESTAMP-INTERVAL '48 hours'
                """,id,version);
    }

    @Override public boolean beginAttempt(Claim c) {
        return jdbc.update("""
                UPDATE notice_translation_job SET attempts=attempts+1
                WHERE notice_id=? AND source_version=? AND lang=? AND owner=? AND state='RUNNING'
                AND attempts<3 AND lease_until>CURRENT_TIMESTAMP
                """,c.noticeId(),c.version(),c.lang().code(),c.owner())==1;
    }

    @Override public void defer(Claim c, long delay) {
        jdbc.update("""
                UPDATE notice_translation_job SET
                state=CASE WHEN attempts>=3 THEN 'FAILED' ELSE 'PENDING' END,
                available_at=CURRENT_TIMESTAMP+(? * INTERVAL '1 second'),owner=NULL,lease_until=NULL
                WHERE notice_id=? AND source_version=? AND lang=? AND owner=? AND state='RUNNING' AND lease_until>CURRENT_TIMESTAMP
                """,Math.max(30,Math.min(86401,delay)),c.noticeId(),c.version(),c.lang().code(),c.owner());
    }

    @Override @Transactional
    public List<Long> dueNotices() {
        jdbc.update("""
                UPDATE notice_translation_job SET state='FAILED',owner=NULL,lease_until=NULL
                WHERE attempts>=3 AND ((state='RUNNING' AND lease_until<CURRENT_TIMESTAMP) OR state='PENDING')
                """);
        jdbc.update("""
                UPDATE notice_translation_job j SET state='CANCELLED',owner=NULL,lease_until=NULL
                WHERE state IN ('PENDING','RUNNING') AND (created_at<CURRENT_TIMESTAMP-INTERVAL '48 hours'
                OR NOT EXISTS(SELECT 1 FROM office_notice n JOIN office o ON o.id=n.office_id
                    WHERE n.id=j.notice_id AND n.version=j.source_version AND n.status<>'ARCHIVED'
                    AND n.expires_at>=CURRENT_DATE AND o.status<>'SUSPENDED'))
                """);
        return jdbc.queryForList("""
                SELECT notice_id FROM notice_translation_job j
                WHERE EXISTS(SELECT 1 FROM office_notice n WHERE n.id=j.notice_id AND n.status='APPROVED')
                AND ((state='PENDING' AND available_at<=CURRENT_TIMESTAMP) OR (state='RUNNING' AND lease_until<CURRENT_TIMESTAMP))
                GROUP BY notice_id ORDER BY min(available_at),notice_id LIMIT 10
                """,Long.class);
    }
}
