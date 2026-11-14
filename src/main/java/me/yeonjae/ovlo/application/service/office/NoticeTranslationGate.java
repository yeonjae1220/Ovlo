package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.port.out.office.TranslationPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 공지 번역 간이 게이트(설계 §5.1). 리포트 재생성 게이트의 축소판:
 * - 비어 있지 않을 것
 * - 원문과 동일하지 않을 것(번역이 안 된 복사)
 * - 날짜는 표기 대신 연·월·일과 출현 횟수를 비교한다(월 이름 번역 허용, 없는 연도 추측 금지).
 * - 날짜를 제외한 숫자는 언어별 소수점·천 단위 구분자를 검증하고 정확한 값·최소 출현 횟수를 보존한다.
 * 실패하면 그 언어 번역은 저장하지 않는다. "확인 못 한 내용은 쓰지 않는다"와 같은 원칙.
 */
@Component
public class NoticeTranslationGate {

    public record Verdict(boolean passed, List<String> reasons) {
        public static Verdict ok() { return new Verdict(true, List.of()); }
    }

    public Verdict check(TranslationPort.Request request, TranslationPort.Result result) {
        List<String> reasons = new ArrayList<>();
        if (result == null || isBlank(result.title()) || isBlank(result.body())) {
            reasons.add("번역 결과가 비어 있습니다");
            return new Verdict(false, reasons);
        }
        if (result.title().trim().equals(request.title().trim()) && result.body().trim().equals(request.body().trim())) {
            reasons.add("번역이 원문과 동일합니다");
        }
        var sourceDates = TranslationDates.extract(request.title() + "\n" + request.body(), request.from());
        var targetDates = TranslationDates.extract(result.title() + "\n" + result.body(), request.to());
        if (sourceDates.invalidDate() || targetDates.invalidDate()) {
            reasons.add("유효하지 않은 날짜가 있습니다");
        }
        if (!sourceDates.dates().equals(targetDates.dates())) {
            reasons.add("원문과 번역의 날짜가 다릅니다: 원문=" + sourceDates.dates() + ", 번역=" + targetDates.dates());
        }
        var sourceTimes = TranslationTimes.extract(sourceDates.remainingText(), request.from());
        var targetTimes = TranslationTimes.extract(targetDates.remainingText(), request.to());
        if (sourceTimes.invalidTime() || targetTimes.invalidTime()) reasons.add("유효하지 않은 시각이 있습니다");
        if (!sourceTimes.times().equals(targetTimes.times())) reasons.add("원문과 번역의 시각 또는 출현 횟수가 다릅니다");
        var sourceNumbers = TranslationNumbers.extract(sourceTimes.remainingText(), request.from());
        var targetNumbers = TranslationNumbers.extract(targetTimes.remainingText(), request.to());
        if (sourceNumbers.invalidNumber() || targetNumbers.invalidNumber()) {
            reasons.add("언어의 소수점·천 단위 표기와 맞지 않는 숫자가 있습니다");
        }
        var missing = sourceNumbers.counts().entrySet().stream()
                .filter(entry -> targetNumbers.counts().getOrDefault(entry.getKey(), 0) < entry.getValue())
                .map(entry -> entry.getKey().toPlainString()).sorted().toList();
        if (!missing.isEmpty()) {
            reasons.add("원문 숫자의 값 또는 출현 횟수가 번역에서 누락되었습니다: " + String.join(", ", missing));
        }
        return reasons.isEmpty() ? Verdict.ok() : new Verdict(false, reasons);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
