package me.yeonjae.ovlo.domain.office.model;

import me.yeonjae.ovlo.domain.office.exception.ConsultationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultationTest {

    private static final Instant NOW = Instant.parse("2027-03-02T09:00:00Z");
    private static final Consultation.SharedContext CTX =
            new Consultation.SharedContext("체류 연장 가능한가요?", null, NoticeLang.VI, true);

    private static Consultation open() {
        return Consultation.open(5L, 42L, 900L, ConsultationConsent.CURRENT_VERSION, CTX, 300L, NOW);
    }

    @Test
    @DisplayName("동의 버전이 없거나 모르는 버전이면 만들 수 없다(CONSENT_REQUIRED); 있으면 정본 문구가 함께 기록된다")
    void consentRequired() {
        assertThatThrownBy(() -> Consultation.open(5L, 42L, null, " ", CTX, 300L, NOW))
                .isInstanceOf(ConsultationException.class)
                .extracting(e -> ((ConsultationException) e).getErrorType())
                .isEqualTo(ConsultationException.ErrorType.CONSENT_REQUIRED);
        assertThatThrownBy(() -> Consultation.open(5L, 42L, null, "v99", CTX, 300L, NOW))
                .isInstanceOf(ConsultationException.class);

        Consultation c = open();
        assertThat(c.getStatus()).isEqualTo(ConsultationStatus.OPEN);
        assertThat(c.getConsentVersion()).isEqualTo("v1");
        assertThat(c.getConsentText()).contains("국제처 담당자에게 전달");
        assertThat(c.getChatRoomId()).isEqualTo(300L);
        assertThat(c.getInquiryId()).isEqualTo(900L);
    }

    @Test
    @DisplayName("배정 → 해결 → 종료 전이와 시각 기록")
    void happyPath() {
        Consultation c = open();

        c.assign(11L, NOW.plusSeconds(60));
        assertThat(c.getStatus()).isEqualTo(ConsultationStatus.IN_PROGRESS);
        assertThat(c.getAssignedStaffMemberId()).isEqualTo(11L);

        c.recordStaffReply(NOW.plusSeconds(120));
        c.recordStaffReply(NOW.plusSeconds(500));
        assertThat(c.getFirstStaffReplyAt()).isEqualTo(NOW.plusSeconds(120));

        c.resolve(NOW.plusSeconds(600));
        assertThat(c.getStatus()).isEqualTo(ConsultationStatus.RESOLVED);
        assertThat(c.isAutoCloseDue(NOW.plusSeconds(600).plus(Duration.ofDays(6)))).isFalse();
        assertThat(c.isAutoCloseDue(NOW.plusSeconds(600).plus(Duration.ofDays(7)))).isTrue();

        c.close(NOW.plusSeconds(700));
        assertThat(c.getStatus()).isEqualTo(ConsultationStatus.CLOSED);
        assertThat(c.getClosedAt()).isEqualTo(NOW.plusSeconds(700));
        assertThat(c.isActive()).isFalse();
    }

    @Test
    @DisplayName("CLOSED 는 되돌릴 수 없고, 학생은 종료 전 언제든 종료할 수 있다(해결 시각이 없으면 종료 시각으로 채운다)")
    void closedIsFinal() {
        Consultation c = open();
        c.close(NOW);
        assertThat(c.getResolvedAt()).isEqualTo(NOW);

        assertThatThrownBy(() -> c.assign(11L, NOW)).isInstanceOf(ConsultationException.class)
                .extracting(e -> ((ConsultationException) e).getErrorType())
                .isEqualTo(ConsultationException.ErrorType.INVALID_STATE);
        assertThatThrownBy(() -> c.resolve(NOW)).isInstanceOf(ConsultationException.class);
        assertThatThrownBy(() -> c.close(NOW)).isInstanceOf(ConsultationException.class);
    }

    @Test
    @DisplayName("RESOLVED 에서는 배정·재해결이 안 되고, IN_PROGRESS 에서 담당자를 바꿀 수 있다")
    void transitionsGuarded() {
        Consultation c = open();
        c.assign(11L, NOW);
        c.assign(12L, NOW);
        assertThat(c.getAssignedStaffMemberId()).isEqualTo(12L);

        c.resolve(NOW);
        assertThatThrownBy(() -> c.assign(13L, NOW)).isInstanceOf(ConsultationException.class);
        assertThatThrownBy(() -> c.resolve(NOW)).isInstanceOf(ConsultationException.class);
        assertThat(c.isOwnedBy(42L)).isTrue();
    }
}
