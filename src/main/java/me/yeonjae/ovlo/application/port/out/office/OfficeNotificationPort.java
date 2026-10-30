package me.yeonjae.ovlo.application.port.out.office;

/** 담당자 초대 등 Office 관련 알림. 구현: LogOfficeNotificationAdapter(기본) / SmtpOfficeNotificationAdapter(spring.mail.host). */
public interface OfficeNotificationPort {
    void sendStaffInvitation(String toEmail, String officeName, String invitedBy);
}
