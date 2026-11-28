package me.yeonjae.ovlo.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import me.yeonjae.ovlo.adapter.out.persistence.repository.CommentJpaRepository;
import me.yeonjae.ovlo.adapter.out.persistence.repository.FollowJpaRepository;
import me.yeonjae.ovlo.adapter.out.persistence.repository.MessageJpaRepository;
import me.yeonjae.ovlo.adapter.out.persistence.repository.PostJpaRepository;
import me.yeonjae.ovlo.application.port.out.member.HideContentByWithdrawnMemberPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class WithdrawalContentHidingAdapter implements HideContentByWithdrawnMemberPort {

    private final me.yeonjae.ovlo.application.port.out.office.OfficePersonalDataPort officeData;
    private final PostJpaRepository postJpaRepository;
    private final CommentJpaRepository commentJpaRepository;
    private final MessageJpaRepository messageJpaRepository;
    private final FollowJpaRepository followJpaRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Override
    @Transactional
    public void hideAllContentByMember(Long memberId) {
        officeData.eraseMember(memberId);
        postJpaRepository.hideAllByAuthorId(memberId);
        commentJpaRepository.hideAllByAuthorId(memberId);
        messageJpaRepository.hideAllBySenderId(memberId);
        followJpaRepository.hideAllByMemberId(memberId);
        jdbc.update("DELETE FROM verification_credential WHERE member_id=?", memberId);
        jdbc.update("DELETE FROM personal_event WHERE member_id=?", memberId);
        jdbc.update("DELETE FROM calendar_subscription WHERE member_id=?", memberId);
        jdbc.update("DELETE FROM university_calendar_manager WHERE member_id=?", memberId);
        jdbc.update("""
            UPDATE calendar_audit SET snapshot='[Personal data erased]'
            WHERE aggregate_type='UNIVERSITY_APPLICATION'
              AND aggregate_id IN (SELECT id FROM university_application WHERE member_id=?)
            """, memberId);
        jdbc.update("""
            UPDATE university_application SET department='[erased]', contact_email='erased@deleted.invalid',
              message='[erased]', review_note=NULL, status='REVOKED', active_slot=NULL, version=version+1
            WHERE member_id=?
            """, memberId);
        jdbc.update("UPDATE office_staff SET status='REMOVED' WHERE member_id=?", memberId);
        // 탈퇴 계정은 복원되지 않으므로 양방향 차단 기록을 남길 이유가 없다(개인정보처리방침: 차단 해제 또는 탈퇴 시까지).
        jdbc.update("DELETE FROM member_block WHERE actor_id=? OR target_id=?", memberId, memberId);
        jdbc.update("""
            INSERT INTO media_deletion_job(media_id) SELECT id FROM media_file WHERE uploader_id=?
            ON CONFLICT (media_id) DO NOTHING
            """, memberId);
        jdbc.update("UPDATE media_file SET original_filename='erased' WHERE uploader_id=?", memberId);
        jdbc.update("UPDATE member SET personal_data_erased_at=CURRENT_TIMESTAMP WHERE id=?", memberId);
    }
}
