package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.port.out.member.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Explicit maintenance operation, disabled unless an operator enables its runner after a backup. */
@Component
public class LegacyMemberErasureAdapter implements LegacyMemberErasurePort {
    private final JdbcTemplate jdbc;
    private final HideContentByWithdrawnMemberPort content;
    public LegacyMemberErasureAdapter(JdbcTemplate jdbc, HideContentByWithdrawnMemberPort content) { this.jdbc=jdbc;this.content=content; }
    @Override public long pendingCount() {
        return jdbc.queryForObject("SELECT count(*) FROM member WHERE status='WITHDRAWN' AND personal_data_erased_at IS NULL", Long.class);
    }
    @Override @Transactional public int eraseBatch(int limit) {
        if (limit<1 || limit>50) throw new IllegalArgumentException("Batch size must be 1..50");
        var ids=jdbc.queryForList("SELECT id FROM member WHERE status='WITHDRAWN' AND personal_data_erased_at IS NULL ORDER BY id LIMIT ? FOR UPDATE SKIP LOCKED",Long.class,limit);
        for (long id : ids) {
            String erased=java.util.UUID.randomUUID().toString().replace("-","");
            jdbc.update("""
                UPDATE member SET nickname=?,name='Withdrawn member',email=?,password=NULL,provider='LOCAL',
                  provider_id=NULL,hometown=NULL,home_university_id=NULL,major_name=NULL,degree_type=NULL,
                  grade_level=NULL,profile_image_media_id=NULL,bio=NULL,birth_date=NULL,role='MEMBER'
                WHERE id=? AND status='WITHDRAWN'
                ""","withdrawn_"+erased,erased+"@deleted.invalid",id);
            jdbc.update("DELETE FROM member_contact_info WHERE member_id=?",id);
            jdbc.update("DELETE FROM member_language_skill WHERE member_id=?",id);
            jdbc.update("DELETE FROM member_university_experience WHERE member_id=?",id);
            content.hideAllContentByMember(id);
        }
        return ids.size();
    }
}
