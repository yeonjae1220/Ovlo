package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.port.out.office.RevokeOfficeChatAccessPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OfficeChatAccessAdapter implements RevokeOfficeChatAccessPort {
    private final JdbcTemplate jdbc;
    public OfficeChatAccessAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void revoke(Long officeId, Long memberId) {
        // Revocation is independent of room size. Keep messages and the student's participation.
        jdbc.update("""
            DELETE FROM chat_room_participant
            WHERE member_id=? AND chat_room_id IN (
                SELECT chat_room_id FROM office_consultation WHERE office_id=? AND member_id<>?)
            AND NOT EXISTS (SELECT 1 FROM office_staff
                            WHERE office_id=? AND member_id=? AND status='ACTIVE')
            """, memberId, officeId, memberId, officeId, memberId);
    }
}
