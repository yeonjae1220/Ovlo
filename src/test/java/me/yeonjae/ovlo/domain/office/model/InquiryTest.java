package me.yeonjae.ovlo.domain.office.model;

import me.yeonjae.ovlo.domain.office.exception.InquiryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InquiryTest {

    private static final Instant NOW = Instant.parse("2027-03-02T09:00:00Z");

    @Test
    @DisplayName("ANSWERED 는 답변·인용이 있고 인용이 후보의 부분집합이어야 한다")
    void answeredRequiresCitationsWithinCandidates() {
        Inquiry ok = Inquiry.answered(5L, 42L, NoticeLang.VI, "기숙사 신청은 언제까지?", "2월 10일까지입니다 [1]",
                List.of(77L, 77L), List.of(77L, 78L), NOW, 820, "fake");
        assertThat(ok.getOutcome()).isEqualTo(InquiryOutcome.ANSWERED);
        assertThat(ok.getCitedNoticeIds()).containsExactly(77L);

        assertThatThrownBy(() -> Inquiry.answered(5L, 42L, NoticeLang.VI, "q", "a", List.of(99L), List.of(77L), NOW, 1, "m"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Inquiry.answered(5L, 42L, NoticeLang.VI, "q", "a", List.of(), List.of(77L), NOW, 1, "m"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Inquiry.answered(5L, 42L, NoticeLang.VI, "q", " ", List.of(77L), List.of(77L), NOW, 1, "m"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("NO_GROUNDS 는 답변·인용 없이 후보만 남긴다")
    void noGrounds() {
        Inquiry i = Inquiry.noGrounds(5L, 42L, NoticeLang.EN, "Can I work 30 hours?", List.of(77L), NOW, 300, "fake");

        assertThat(i.getOutcome()).isEqualTo(InquiryOutcome.NO_GROUNDS);
        assertThat(i.getAnswerText()).isNull();
        assertThat(i.getCitedNoticeIds()).isEmpty();
        assertThat(i.getCandidateNoticeIds()).containsExactly(77L);
    }

    @Test
    @DisplayName("질문은 비어 있을 수 없고 1,000자를 넘을 수 없다 (BAD_REQUEST)")
    void questionLimits() {
        assertThatThrownBy(() -> Inquiry.validateQuestion("  "))
                .isInstanceOf(InquiryException.class)
                .extracting(e -> ((InquiryException) e).getErrorType())
                .isEqualTo(InquiryException.ErrorType.BAD_REQUEST);
        assertThatThrownBy(() -> Inquiry.validateQuestion("x".repeat(1001))).isInstanceOf(InquiryException.class);
        assertThat(Inquiry.validateQuestion("  질문 ")).isEqualTo("질문");
    }

    @Test
    @DisplayName("이관 표시와 소유자 확인")
    void escalationAndOwnership() {
        Inquiry i = Inquiry.noGrounds(5L, 42L, NoticeLang.KO, "q", List.of(), NOW, 1, "m");

        assertThat(i.isOwnedBy(42L)).isTrue();
        assertThat(i.isOwnedBy(43L)).isFalse();
        i.markEscalated();
        assertThat(i.getOutcome()).isEqualTo(InquiryOutcome.ESCALATED);
    }

    @Test
    @DisplayName("피드백 사유는 공백이면 null, 300자 초과는 거부")
    void feedback() {
        assertThat(new InquiryFeedback(1L, true, "  ", NOW).reason()).isNull();
        assertThat(new InquiryFeedback(1L, false, " 틀림 ", NOW).reason()).isEqualTo("틀림");
        assertThatThrownBy(() -> new InquiryFeedback(1L, false, "x".repeat(301), NOW)).isInstanceOf(IllegalArgumentException.class);
    }
}
