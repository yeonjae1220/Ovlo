package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.port.out.office.TranslationPort;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "EN | December 10, 2026", "EN | 10 December 2026", "EN | Dec. 10th, 2026",
            "DE | 10. Dezember 2026", "DE | 10. Dez. 2026", "DE | 10.12.2026",
            "FR | 10 décembre 2026", "FR | 10 déc. 2026", "FR | 10/12/2026",
            "JA | 2026年12月10日", "ZH | 2026年12月10日", "VI | ngày 10 tháng 12 năm 2026",
            "EN | 2026-12-10"
    })
    void acceptsEquivalentCompleteDates(NoticeLang lang, String date) {
        var source = new TranslationPort.Request(NoticeLang.KO, lang, "기숙사 신청",
                "마감: 2026년 12월 10일 18시. 보증금 150,000원.");
        assertThat(gate.check(source, new TranslationPort.Result("Application",
                "Deadline: " + date + " 18:00. Deposit 150000 KRW.", "m")).passed()).isTrue();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "EN | February 10", "DE | 10. Februar", "FR | 10 février"
    })
    void acceptsMonthNameWithoutInventingYear(NoticeLang lang, String date) {
        var source = new TranslationPort.Request(NoticeLang.KO, lang, "신청", "2월 10일 마감");
        assertThat(gate.check(source, new TranslationPort.Result("Application", date, "m")).passed()).isTrue();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "December 11, 2026", "November 10, 2026", "December 10, 2027",
            "December 10", "December 10, 2026; December 11, 2026", "No deadline"
    })
    void rejectsChangedOmittedOrAddedDatesEvenWhenNumbersExistElsewhere(String translatedDate) {
        var source = new TranslationPort.Request(NoticeLang.KO, NoticeLang.EN, "안내",
                "마감 2026년 12월 10일. 참고 번호 2026, 12, 10, 11, 2027.");
        var target = new TranslationPort.Result("Notice", translatedDate + ". Reference numbers 2026, 12, 10, 11, 2027.", "m");
        assertThat(gate.check(source, target).passed()).isFalse();
    }

    @Test void dateDigitsCannotHideMissingAmountsOrDurations() {
        var source = new TranslationPort.Request(NoticeLang.KO, NoticeLang.EN, "신청",
                "2026년 12월 10일 마감. 비용 12원. 소요 10일.");
        assertThat(gate.check(source, new TranslationPort.Result("Apply", "December 10, 2026", "m")).passed()).isFalse();
    }

    @Test void rejectsDuplicateDateOmission() {
        var source = new TranslationPort.Request(NoticeLang.KO, NoticeLang.EN, "신청",
                "신청 2026년 12월 10일. 납부 2026년 12월 10일.");
        assertThat(gate.check(source, new TranslationPort.Result("Apply", "December 10, 2026", "m")).passed()).isFalse();
    }

    @Test void rejectsInvalidCalendarDate() {
        var source = new TranslationPort.Request(NoticeLang.KO, NoticeLang.EN, "마감", "2026년 2월 28일");
        assertThat(gate.check(source, new TranslationPort.Result("Deadline", "February 30, 2026 (28)", "m")).passed()).isFalse();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "EN | 2024년 2월 29일 | February 29, 2024",
            "DE | 2026년 3월 1일 | 1. März 2026",
            "FR | 2026년 8월 1일 | 1er août 2026",
            "FR | 2026년 2월 10일 | 10 FÉVRIER 2026",
            "EN | 2월 29일 | February 29"
    })
    void handlesLeapDaysAccentsAndOrdinals(NoticeLang lang, String original, String translated) {
        var source = new TranslationPort.Request(NoticeLang.KO, lang, "마감", original);
        assertThat(gate.check(source, new TranslationPort.Result("Deadline", translated, "m")).passed()).isTrue();
    }

    @Test void keepsNonDateMonthsSubjectToNumberValidation() {
        var source = new TranslationPort.Request(NoticeLang.KO, NoticeLang.EN, "안내", "기간은 12개월, 비용은 150,000원");
        assertThat(gate.check(source, new TranslationPort.Result("Notice", "Duration: December. Fee: 150000 KRW", "m")).passed()).isFalse();
    }

    @Test void doesNotInventYearForYearlessDate() {
        var source = new TranslationPort.Request(NoticeLang.KO, NoticeLang.EN, "마감", "12월 10일");
        assertThat(gate.check(source, new TranslationPort.Result("Deadline", "December 10, 2026", "m")).passed()).isFalse();
    }

}
