package me.yeonjae.ovlo.application.port.out.office;

import java.time.LocalDate;
import java.util.List;

/** Office 관련 알림. 구현: LogOfficeNotificationAdapter(기본) / SmtpOfficeNotificationAdapter(spring.mail.host). */
public interface OfficeNotificationPort {
    void sendStaffInvitation(String toEmail, String officeName, String invitedBy);

    /** 만료 7일 전 안내. 공지마다 한 번. */
    void sendNoticeExpiringSoon(List<String> toEmails, String officeName, String noticeTitle, LocalDate expiresAt);

    /** 학생이 상담을 열었다 — 담당자 전원에게. 질문 본문만 싣고 학생 식별 정보는 싣지 않는다. */
    void sendConsultationOpened(List<String> toEmails, String officeName, String question);

    /** 일부 언어 번역이 게이트에 걸려 생성되지 않았다. */
    void sendNoticeTranslationFailed(List<String> toEmails, String officeName, String noticeTitle, List<String> failedLangs);
}
