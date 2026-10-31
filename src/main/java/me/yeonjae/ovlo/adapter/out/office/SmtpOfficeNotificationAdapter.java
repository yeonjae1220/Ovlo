package me.yeonjae.ovlo.adapter.out.office;

import me.yeonjae.ovlo.application.port.out.office.OfficeNotificationPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

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
        send(List.of(toEmail), "[Ovlo] " + officeName + " 담당자로 초대되었습니다", """
                %s 담당자로 초대되었습니다.

                Ovlo 에 로그인한 뒤 아래 주소에서 초대를 수락하면 담당자 화면을 쓸 수 있습니다.
                %s/office

                초대한 사람: %s
                본인이 요청한 적이 없다면 이 메일을 무시해 주세요.
                """.formatted(officeName, appBaseUrl, invitedBy));
    }

    @Override
    public void sendNoticeExpiringSoon(List<String> toEmails, String officeName, String noticeTitle, LocalDate expiresAt) {
        if (toEmails.isEmpty()) return;
        send(toEmails, "[Ovlo] 공지 만료 예정: " + noticeTitle, """
                %s 의 승인 공지가 곧 만료됩니다.

                제목: %s
                만료일: %s

                만료되면 학생 안내의 근거에서 자동으로 빠집니다. 계속 유효하면 담당자 화면에서 만료일을 갱신하고 다시 승인하세요.
                %s/office/notices
                """.formatted(officeName, noticeTitle, expiresAt, appBaseUrl));
    }

    @Override
    public void sendNoticeTranslationFailed(List<String> toEmails, String officeName, String noticeTitle,
                                            List<String> failedLangs) {
        if (toEmails.isEmpty()) return;
        send(toEmails, "[Ovlo] 공지 번역 일부 실패: " + noticeTitle, """
                %s 의 공지 번역 중 일부 언어가 검사에 걸려 생성되지 않았습니다.

                제목: %s
                실패 언어: %s

                해당 언어 학생에게는 이 공지가 근거로 쓰이지 않습니다. 담당자 화면에서 "다시 번역"을 누르거나 원문을 손봐 주세요.
                %s/office/notices
                """.formatted(officeName, noticeTitle, String.join(", ", failedLangs), appBaseUrl));
    }

    private void send(List<String> to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to.toArray(String[]::new));
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
