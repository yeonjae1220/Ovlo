package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.office.model.Consultation;
import me.yeonjae.ovlo.domain.office.model.ConsultationStatus;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;

import java.time.Instant;

/** 학생·담당자 공통 상담 요약/상세. 학생 식별 정보(이메일)는 없다. */
public record ConsultationResult(Long id, Long officeId, ConsultationStatus status, Long inquiryId, String question,
                                 String answerText, NoticeLang lang, boolean verifiedStudent, Long chatRoomId,
                                 Long assignedStaffMemberId, String assignedStaffNickname, String consentVersion,
                                 Instant openedAt, Instant firstStaffReplyAt, Instant resolvedAt, Instant closedAt) {

    public static ConsultationResult from(Consultation c, String assignedStaffNickname) {
        var ctx = c.getSharedContext();
        return new ConsultationResult(c.getId(), c.getOfficeId(), c.getStatus(), c.getInquiryId(), ctx.question(),
                ctx.answerText(), ctx.lang(), ctx.verifiedStudent(), c.getChatRoomId(), c.getAssignedStaffMemberId(),
                assignedStaffNickname, c.getConsentVersion(), c.getOpenedAt(), c.getFirstStaffReplyAt(), c.getResolvedAt(),
                c.getClosedAt());
    }
}
