package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.port.out.office.OfficePersonalDataPort;
import me.yeonjae.ovlo.domain.office.exception.InquiryException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;

/** All writers and erasure share member -> inquiry/consultation -> message row locks. */
@Component
public class OfficePersonalDataAdapter implements OfficePersonalDataPort {
    public static final String DELETED="[Content deleted]";
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    public OfficePersonalDataAdapter(JdbcTemplate jdbc,TransactionTemplate tx) { this.jdbc=jdbc;this.tx=tx; }

    public void lockActiveMember(long id) {
        String state=jdbc.queryForObject("SELECT status FROM member WHERE id=? FOR NO KEY UPDATE",String.class,id);
        if (!"ACTIVE".equals(state)) throw unavailable();
    }
    public void lockInquiry(long id) {
        Long member=jdbc.queryForObject("SELECT member_id FROM office_inquiry WHERE id=?",Long.class,id);
        lockActiveMember(member);
        Boolean valid=jdbc.queryForObject("SELECT personal_data_erased_at IS NULL AND (personal_data_expires_at IS NULL OR personal_data_expires_at>CURRENT_TIMESTAMP) FROM office_inquiry WHERE id=? FOR UPDATE",Boolean.class,id);
        if(!Boolean.TRUE.equals(valid)) throw unavailable();
    }
    public void lockConsultation(long id) {
        Long member=jdbc.queryForObject("SELECT member_id FROM office_consultation WHERE id=?",Long.class,id);
        lockActiveMember(member);
        Boolean valid=jdbc.queryForObject("SELECT personal_data_erased_at IS NULL AND (personal_data_expires_at IS NULL OR personal_data_expires_at>CURRENT_TIMESTAMP) FROM office_consultation WHERE id=? FOR UPDATE",Boolean.class,id);
        if(!Boolean.TRUE.equals(valid)) throw unavailable();
    }
    public void lockRoom(long room) {
        for(Long id:jdbc.queryForList("SELECT id FROM office_consultation WHERE chat_room_id=?",Long.class,room)) lockConsultation(id);
    }
    public void lockMessage(long id) {
        Long room=jdbc.queryForObject("SELECT chat_room_id FROM message WHERE id=?",Long.class,id);
        lockRoom(room);
        if(!Boolean.FALSE.equals(jdbc.queryForObject("SELECT hidden_by_withdrawal FROM message WHERE id=? FOR UPDATE",Boolean.class,id))) throw unavailable();
    }
    private static InquiryException unavailable() {
        return new InquiryException("삭제되었거나 더 이상 사용할 수 없는 대화입니다",InquiryException.ErrorType.CONFLICT);
    }

    @Override @Transactional
    public void eraseMember(long member) {
        jdbc.queryForObject("SELECT id FROM member WHERE id=? FOR UPDATE",Long.class,member);
        erase(member,true);
    }
    @Override public int eraseExpiredBatch() {
        var members=jdbc.queryForList("""
            SELECT member_id FROM (
              SELECT i.member_id FROM office_inquiry i JOIN member m ON m.id=i.member_id
                WHERE i.personal_data_erased_at IS NULL AND (i.personal_data_expires_at<=CURRENT_TIMESTAMP OR m.status='WITHDRAWN')
              UNION SELECT c.member_id FROM office_consultation c JOIN member m ON m.id=c.member_id
                WHERE c.personal_data_erased_at IS NULL AND (c.personal_data_expires_at<=CURRENT_TIMESTAMP OR m.status='WITHDRAWN')
            ) due ORDER BY member_id LIMIT 100
            """,Long.class);
        for(long member:members) tx.executeWithoutResult(status->{
            String state=jdbc.queryForObject("SELECT status FROM member WHERE id=? FOR NO KEY UPDATE",String.class,member);
            erase(member,"WITHDRAWN".equals(state));
        });
        return members.size();
    }
    private void erase(long member,boolean all) {
        var inquiries=jdbc.queryForList("SELECT id FROM office_inquiry WHERE member_id=? AND personal_data_erased_at IS NULL AND (? OR personal_data_expires_at<=CURRENT_TIMESTAMP) ORDER BY id FOR UPDATE",Long.class,member,all);
        for(long id:inquiries) {
            jdbc.update("DELETE FROM office_inquiry_feedback WHERE inquiry_id=?",id);
            jdbc.update("DELETE FROM office_inquiry_evidence WHERE id=?",id);
            jdbc.update("UPDATE office_inquiry SET question=?,answer_text=NULL,cited_notice_ids=NULL,candidate_notice_ids=NULL,personal_data_erased_at=CURRENT_TIMESTAMP WHERE id=?",DELETED,id);
        }
        var consultations=jdbc.queryForList("SELECT id FROM office_consultation WHERE member_id=? AND personal_data_erased_at IS NULL AND (? OR personal_data_expires_at<=CURRENT_TIMESTAMP) ORDER BY id FOR UPDATE",Long.class,member,all);
        for(long id:consultations) {
            Long room=jdbc.queryForObject("SELECT chat_room_id FROM office_consultation WHERE id=?",Long.class,id);
            // Both sides may quote the student's personal details; erase the whole consented room.
            jdbc.queryForList("SELECT id FROM message WHERE chat_room_id=? ORDER BY id FOR UPDATE",Long.class,room);
            jdbc.update("DELETE FROM chat_message_translation WHERE message_id IN (SELECT id FROM message WHERE chat_room_id=?)",room);
            jdbc.update("UPDATE message SET content=?,hidden_by_withdrawal=true WHERE chat_room_id=?",DELETED,room);
            jdbc.update("UPDATE office_consultation SET shared_question=?,shared_answer=NULL,shared_conversation=NULL,status='CLOSED',closed_at=COALESCE(closed_at,CURRENT_TIMESTAMP),personal_data_erased_at=CURRENT_TIMESTAMP WHERE id=?",DELETED,id);
        }
    }
}
