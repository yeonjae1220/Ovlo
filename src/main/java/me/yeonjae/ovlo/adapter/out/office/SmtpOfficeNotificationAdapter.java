package me.yeonjae.ovlo.adapter.out.office;

import me.yeonjae.ovlo.application.port.out.office.OfficeNotificationPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** 운영용 SMTP. spring.mail.host 가 있으면 Log 어댑터 대신 이쪽이 주입된다(@Primary, SmtpEmailSenderAdapter 와 같은 방식). */
@Component
@Primary
@ConditionalOnProperty(name = "spring.mail.host")
public class SmtpOfficeNotificationAdapter implements OfficeNotificationPort {

    private final JavaMailSender mailSender;
    private final String from;
    private final String appBaseUrl;

    public SmtpOfficeNotificationAdapter(JavaMailSender mailSender,
                                         @Value("${ovlo.mail.from:no-reply@ovlo.app}") String from,
                                         @Value("${ovlo.app.base-url:https://ovlo.mungji.com}") String appBaseUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.appBaseUrl = appBaseUrl;
    }

    @Override
    public void sendStaffInvitation(String toEmail, String officeName, String invitedBy) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("[Ovlo] " + officeName + " 담당자로 초대되었습니다");
        message.setText("""
                %s 담당자로 초대되었습니다.

                Ovlo 에 로그인한 뒤 아래 주소에서 초대를 수락하면 담당자 화면을 쓸 수 있습니다.
                %s/office

                초대한 사람: %s
                본인이 요청한 적이 없다면 이 메일을 무시해 주세요.
                """.formatted(officeName, appBaseUrl, invitedBy));
        mailSender.send(message);
    }
}
