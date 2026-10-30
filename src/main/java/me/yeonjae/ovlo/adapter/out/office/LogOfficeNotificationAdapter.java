package me.yeonjae.ovlo.adapter.out.office;

import me.yeonjae.ovlo.application.port.out.office.OfficeNotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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
}
