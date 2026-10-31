package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.port.out.office.TranslationPort;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeTranslationGateTest {

    private final NoticeTranslationGate gate = new NoticeTranslationGate();
    private final TranslationPort.Request request = new TranslationPort.Request(NoticeLang.KO, NoticeLang.EN,
            "기숙사 신청 안내", "신청 기간은 2월 10일까지, 보증금은 150,000원입니다.");

    @Test
    @DisplayName("숫자가 모두 남아 있고 원문과 다르면 통과한다 (1,000 과 1000 은 같은 숫자)")
    void passesWhenNumbersPreserved() {
        var result = new TranslationPort.Result("Dorm application",
                "Apply by February 10; the deposit is 150000 KRW (2 months).", "m");

        assertThat(gate.check(request, result).passed()).isTrue();
    }

    @Test
    @DisplayName("원문 숫자가 빠지면 거부하고 어떤 숫자인지 말한다")
    void rejectsMissingNumber() {
        var result = new TranslationPort.Result("Dorm application", "Apply by February; the deposit is 150,000 KRW.", "m");

        var verdict = gate.check(request, result);

        assertThat(verdict.passed()).isFalse();
        assertThat(verdict.reasons()).anySatisfy(r -> assertThat(r).contains("2").contains("10"));
    }

    @Test
    @DisplayName("빈 결과나 원문 복사는 거부한다")
    void rejectsBlankOrIdentical() {
        assertThat(gate.check(request, new TranslationPort.Result("", "x", "m")).passed()).isFalse();
        assertThat(gate.check(request, null).passed()).isFalse();
        assertThat(gate.check(request, new TranslationPort.Result(request.title(), request.body(), "m")).passed()).isFalse();
    }
}
