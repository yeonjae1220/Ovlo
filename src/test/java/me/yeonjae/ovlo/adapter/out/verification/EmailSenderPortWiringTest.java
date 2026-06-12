package me.yeonjae.ovlo.adapter.out.verification;

import me.yeonjae.ovlo.application.port.out.verification.EmailSenderPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * EmailSenderPort 빈 배선 검증.
 *
 * <p>LogEmailSenderAdapter는 {@code matchIfMissing=true}라 spring.mail.host가 설정돼도 함께 활성화된다.
 * 이때 EmailSenderPort 빈이 2개가 되어 주입이 모호해지는 문제(운영 크래시)를 방지하기 위해
 * SmtpEmailSenderAdapter에 {@code @Primary}를 둔다. 이 테스트는 그 계약을 고정한다.</p>
 */
class EmailSenderPortWiringTest {

    @Test
    @DisplayName("spring.mail.host 설정 시 EmailSenderPort는 SMTP 어댑터로 단일 주입된다")
    void smtpPrimaryWhenHostSet() {
        new ApplicationContextRunner()
                .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
                .withUserConfiguration(LogEmailSenderAdapter.class, SmtpEmailSenderAdapter.class)
                .withPropertyValues("spring.mail.host=smtp.example.com")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBean(EmailSenderPort.class)).isInstanceOf(SmtpEmailSenderAdapter.class);
                });
    }

    @Test
    @DisplayName("spring.mail.host 미설정 시 Log 어댑터가 단독 사용된다")
    void logWhenHostMissing() {
        new ApplicationContextRunner()
                .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
                .withUserConfiguration(LogEmailSenderAdapter.class, SmtpEmailSenderAdapter.class)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBean(EmailSenderPort.class)).isInstanceOf(LogEmailSenderAdapter.class);
                });
    }
}
