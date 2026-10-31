package me.yeonjae.ovlo.application.port.out.office;

import java.time.LocalDate;
import java.util.List;

/** Office 관련 알림. 구현: LogOfficeNotificationAdapter(기본) / SmtpOfficeNotificationAdapter(spring.mail.host). */
public interface OfficeNotificationPort {
    void sendStaffInvitation(String toEmail, String officeName, String invitedBy);

    /** 만료 7일 전 안내. 공지마다 한 번. */
    void sendNoticeExpiringSoon(List<String> toEmails, String officeName, String noticeTitle, LocalDate expiresAt);

    /** 일부 언어 번역이 게이트에 걸려 생성되지 않았다. */
    void sendNoticeTranslationFailed(List<String> toEmails, String officeName, String noticeTitle, List<String> failedLangs);
}
