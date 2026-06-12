package me.yeonjae.ovlo.adapter.out.verification;

import me.yeonjae.ovlo.application.port.out.verification.EmailSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 개발/기본용 이메일 발송 어댑터 — 실제 발송 대신 코드를 로그로 출력한다.
 * spring.mail.host가 설정되지 않은 환경에서만 활성화된다(미설정 시 기본).
 * ⚠️ prod에서 실사용자가 인증하려면 SmtpEmailSenderAdapter(또는 메일 API)가 필요하다.
 */
@Component
@ConditionalOnProperty(name = "spring.mail.host", matchIfMissing = true)
public class LogEmailSenderAdapter implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(LogEmailSenderAdapter.class);

    @Override
    public void sendVerificationCode(String toEmail, String code, String universityName) {
        log.info("[EMAIL:VERIFICATION] to={} university={} code={} (LOG adapter — 실제 발송 안 함)",
                toEmail, universityName, code);
    }
}
