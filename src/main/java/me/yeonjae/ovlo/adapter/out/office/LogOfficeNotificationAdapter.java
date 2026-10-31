package me.yeonjae.ovlo.adapter.out.office;

import me.yeonjae.ovlo.application.port.out.office.OfficeNotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/** spring.mail.host 미설정(개발·테스트) 환경용. 발송 대신 로그만 남긴다. */
@Component
@ConditionalOnProperty(name = "spring.mail.host", matchIfMissing = true)
public class LogOfficeNotificationAdapter implements OfficeNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LogOfficeNotificationAdapter.class);

    @Override
    public void sendStaffInvitation(String toEmail, String officeName, String invitedBy) {
        log.info("[EMAIL:OFFICE_INVITE] to={} office={} by={} (LOG adapter — 실제 발송 안 함)",
                toEmail, officeName, invitedBy);
    }

    @Override
    public void sendNoticeExpiringSoon(List<String> toEmails, String officeName, String noticeTitle, LocalDate expiresAt) {
        log.info("[EMAIL:NOTICE_EXPIRING] to={} office={} notice=\"{}\" expiresAt={} (LOG adapter)",
                toEmails, officeName, noticeTitle, expiresAt);
    }

    @Override
    public void sendConsultationOpened(List<String> toEmails, String officeName, String question) {
        log.info("[EMAIL:CONSULTATION_OPENED] to={} office={} question=\"{}\" (LOG adapter)", toEmails, officeName, question);
    }

    @Override
    public void sendNoticeTranslationFailed(List<String> toEmails, String officeName, String noticeTitle,
                                            List<String> failedLangs) {
        log.warn("[EMAIL:NOTICE_TRANSLATION_FAILED] to={} office={} notice=\"{}\" langs={} (LOG adapter)",
                toEmails, officeName, noticeTitle, failedLangs);
    }
}
