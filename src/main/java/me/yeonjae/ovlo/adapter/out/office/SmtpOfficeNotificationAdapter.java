package me.yeonjae.ovlo.adapter.out.office;

import me.yeonjae.ovlo.application.port.out.office.OfficeNotificationPort;
import me.yeonjae.ovlo.domain.office.exception.OfficeNotificationException;
import me.yeonjae.ovlo.shared.metrics.OfficeMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 운영용 SMTP. spring.mail.host 가 있으면 Log 어댑터 대신 이쪽이 주입된다(@Primary, SmtpEmailSenderAdapter 와 같은 방식).
 *
 * <p>전송 실패는 {@code ovlo.office.notification{result=error}} 로 세고 {@link OfficeNotificationException} 으로 바꿔 던진다.
 * 2026-10-31 운영에서 Gmail 이 535 로 인증을 거부하자 날것의 MailException 이 담당자 초대 트랜잭션을 뒤집고
 * 시드 기동을 죽였다 — 어느 호출부가 잡고 어느 호출부가 전파할지는 유스케이스가 정한다(런북 §7).
 * 로그에는 수신 주소·본문·제목을 싣지 않는다(SMTP 응답문에 주소가 섞여 들어오므로 예외 메시지도 인증 실패일 때 첫 줄만).
 */
@Component
@Primary
@ConditionalOnProperty(name = "spring.mail.host")
public class SmtpOfficeNotificationAdapter implements OfficeNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(SmtpOfficeNotificationAdapter.class);
    private static final int DETAIL_MAX = 120;

    private final JavaMailSender mailSender;
    private final OfficeMetrics metrics;
    private final String from;
    private final String appBaseUrl;

    public SmtpOfficeNotificationAdapter(JavaMailSender mailSender, OfficeMetrics metrics,
                                         @Value("${ovlo.mail.from:no-reply@ovlo.app}") String from,
                                         @Value("${ovlo.app.base-url:https://ovlo.mungji.com}") String appBaseUrl) {
        this.mailSender = mailSender;
        this.metrics = metrics;
        this.from = from;
        this.appBaseUrl = appBaseUrl;
    }

    @Override
    public void sendStaffInvitation(String toEmail, String officeName, String invitedBy) {
        send("staff_invitation", List.of(toEmail), "[Ovlo] " + officeName + " 담당자로 초대되었습니다", """
                %s 담당자로 초대되었습니다.

                Ovlo 에 로그인한 뒤 아래 주소에서 초대를 수락하면 담당자 화면을 쓸 수 있습니다.
                %s/office

                초대한 사람: %s
                본인이 요청한 적이 없다면 이 메일을 무시해 주세요.
                """.formatted(officeName, appBaseUrl, invitedBy));
    }

    @Override
    public void sendNoticeExpiringSoon(List<String> toEmails, String officeName, String noticeTitle, LocalDate expiresAt) {
        send("notice_expiring", toEmails, "[Ovlo] 공지 만료 예정: " + noticeTitle, """
                %s 의 승인 공지가 곧 만료됩니다.

                제목: %s
                만료일: %s

                만료되면 학생 안내의 근거에서 자동으로 빠집니다. 계속 유효하면 담당자 화면에서 만료일을 갱신하고 다시 승인하세요.
                %s/office/notices
                """.formatted(officeName, noticeTitle, expiresAt, appBaseUrl));
    }

    @Override
    public void sendConsultationOpened(List<String> toEmails, String officeName, String question) {
        send("consultation_opened", toEmails, "[Ovlo] 새 상담 요청: " + officeName, """
                학생이 담당자 상담을 요청했습니다.

                질문: %s

                담당자 화면의 받은함에서 "맡기"를 누르면 상담방에서 답할 수 있습니다. 학생 메시지는 한국어로 번역되어 함께 보입니다.
                %s/office/consultations
                """.formatted(question, appBaseUrl));
    }

    @Override
    public void sendNoticeTranslationFailed(List<String> toEmails, String officeName, String noticeTitle,
                                            List<String> failedLangs) {
        send("notice_translation_failed", toEmails, "[Ovlo] 공지 번역 일부 실패: " + noticeTitle, """
                %s 의 공지 번역 중 일부 언어가 검사에 걸려 생성되지 않았습니다.

                제목: %s
                실패 언어: %s

                해당 언어 학생에게는 이 공지가 근거로 쓰이지 않습니다. 담당자 화면에서 "다시 번역"을 누르거나 원문을 손봐 주세요.
                %s/office/notices
                """.formatted(officeName, noticeTitle, String.join(", ", failedLangs), appBaseUrl));
    }

    /** 받는 사람이 없으면 아무것도 하지 않는다(보내지도 세지도 않음). */
    private void send(String kind, List<String> to, String subject, String text) {
        if (to.isEmpty()) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to.toArray(String[]::new));
        message.setSubject(subject);
        message.setText(text);
        try {
            mailSender.send(message);
            metrics.notification(kind, "ok");
        } catch (MailException e) {
            metrics.notification(kind, "error");
            log.error("[office] 알림 메일 발송 실패 kind={} recipients={} cause={}{} — 인증 실패(535)면 MAIL_PASSWORD 시크릿을 갱신한다(런북 §7)",
                    kind, to.size(), e.getClass().getSimpleName(), authDetail(e));
            throw new OfficeNotificationException(kind, e);
        }
    }

    /** 인증 실패일 때만 SMTP 응답 첫 줄을 덧붙인다 — 전송 실패 메시지는 수신 주소를 품으므로 싣지 않는다. */
    private static String authDetail(MailException e) {
        if (!(e instanceof MailAuthenticationException) || e.getMessage() == null) return "";
        String first = e.getMessage().strip().split("\\R", 2)[0];
        return " detail='" + (first.length() > DETAIL_MAX ? first.substring(0, DETAIL_MAX) : first) + "'";
    }
}
