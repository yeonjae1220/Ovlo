package me.yeonjae.ovlo.adapter.out.office;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import me.yeonjae.ovlo.domain.office.exception.OfficeNotificationException;
import me.yeonjae.ovlo.shared.metrics.OfficeMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 2026-10-31 운영에서 Gmail 535 가 날것의 MailException 으로 담당자 초대 트랜잭션을 뒤집고 시드 기동을 죽였다.
 * 어댑터는 실패를 세고(메트릭) 도메인 예외로 바꿔 던지며, 삼킬지 전파할지는 유스케이스가 정한다.
 */
@ExtendWith(MockitoExtension.class)
class SmtpOfficeNotificationAdapterTest {

    @Mock JavaMailSender mailSender;
    private SimpleMeterRegistry registry;
    private SmtpOfficeNotificationAdapter adapter;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        adapter = new SmtpOfficeNotificationAdapter(mailSender, new OfficeMetrics(registry),
                "no-reply@ovlo.app", "https://ovlo.example");
    }

    @Test
    @DisplayName("SMTP 인증 실패는 kind 가 담긴 OfficeNotificationException 으로 바뀌고 result=error 로 집계된다")
    void authFailureBecomesDomainException() {
        willThrow(new MailAuthenticationException("535-5.7.8 Username and Password not accepted\nmore lines"))
                .given(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> adapter.sendStaffInvitation("admin@univ.ac.kr", "경북대학교 국제처", "seed"))
                .isInstanceOf(OfficeNotificationException.class)
                .hasCauseInstanceOf(MailAuthenticationException.class)
                .extracting(e -> ((OfficeNotificationException) e).getKind()).isEqualTo("staff_invitation");

        assertThat(registry.get("ovlo.office.notification").tag("kind", "staff_invitation").tag("result", "error")
                .counter().count()).isEqualTo(1);
        assertThat(registry.find("ovlo.office.notification").tag("result", "ok").counter()).isNull();
    }

    @Test
    @DisplayName("전송 실패(MailSendException)도 같은 예외·집계이고 kind 는 호출한 알림 종류다")
    void sendFailureBecomesDomainException() {
        willThrow(new MailSendException("550 5.1.1 someone@x.invalid unknown"))
                .given(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> adapter.sendConsultationOpened(List.of("a@x", "b@x"), "국제처", "질문"))
                .isInstanceOf(OfficeNotificationException.class)
                .extracting(e -> ((OfficeNotificationException) e).getKind()).isEqualTo("consultation_opened");
        assertThat(registry.get("ovlo.office.notification").tag("kind", "consultation_opened").tag("result", "error")
                .counter().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("성공하면 from·to·제목이 채워진 메일이 나가고 result=ok 로 집계된다")
    void successSendsAndCounts() {
        adapter.sendNoticeExpiringSoon(List.of("s1@x", "s2@x"), "국제처", "기숙사 안내", LocalDate.of(2027, 3, 9));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getFrom()).isEqualTo("no-reply@ovlo.app");
        assertThat(sent.getTo()).containsExactly("s1@x", "s2@x");
        assertThat(sent.getSubject()).contains("기숙사 안내");
        assertThat(sent.getText()).contains("2027-03-09").contains("https://ovlo.example/office/notices");
        assertThat(registry.get("ovlo.office.notification").tag("kind", "notice_expiring").tag("result", "ok")
                .counter().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("받는 사람이 없으면 보내지도 집계하지도 않는다")
    void emptyRecipientsSkipped() {
        adapter.sendNoticeTranslationFailed(List.of(), "국제처", "제목", List.of("EN"));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        assertThat(registry.find("ovlo.office.notification").counter()).isNull();
    }
}
