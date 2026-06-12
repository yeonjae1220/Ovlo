package me.yeonjae.ovlo.application.port.out.verification;

/**
 * 인증 코드 이메일 발송. 구현 교체 가능:
 * LogEmailSenderAdapter(dev) → SmtpEmailSenderAdapter(prod).
 */
public interface EmailSenderPort {
    void sendVerificationCode(String toEmail, String code, String universityName);
}
