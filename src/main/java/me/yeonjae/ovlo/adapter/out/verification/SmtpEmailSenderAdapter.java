package me.yeonjae.ovlo.adapter.out.verification;

import me.yeonjae.ovlo.application.port.out.verification.EmailSenderPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 운영용 SMTP 이메일 발송 어댑터. spring.mail.host가 설정된 경우에만 활성화.
 *
 * <p>{@code @Primary}: spring.mail.host가 설정되면 LogEmailSenderAdapter(matchIfMissing=true이라
 * host 존재 시에도 함께 활성화됨)와 EmailSenderPort 빈이 2개가 되므로, 운영 발송기인 이쪽을
 * 우선 주입하도록 한다. host 미설정 시에는 이 빈이 생성되지 않아 Log 어댑터가 단독 사용된다.</p>
 */
@Component
@Primary
@ConditionalOnProperty(name = "spring.mail.host")
public class SmtpEmailSenderAdapter implements EmailSenderPort {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailSenderAdapter(JavaMailSender mailSender,
                                  @Value("${ovlo.mail.from:no-reply@ovlo.app}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendVerificationCode(String toEmail, String code, String universityName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("[Ovlo] 학교 이메일 인증 코드");
        message.setText("""
                %s 인증을 위한 코드입니다.

                인증 코드: %s

                코드는 10분간 유효합니다. 본인이 요청하지 않았다면 이 메일을 무시하세요.
                """.formatted(universityName == null || universityName.isBlank() ? "학교 이메일" : universityName, code));
        mailSender.send(message);
    }
}
