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
 * - 날짜를 제외한 숫자는 언어별 소수점·천 단위 구분자를 검증하고 정확한 값·동일한 출현 횟수를 보존한다.
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
        String sourceText=request.title()+"\n"+request.body();
        String targetText=result.title()+"\n"+result.body();
        if (!contacts(sourceText).equals(contacts(targetText))) {
            reasons.add("원문과 번역의 URL 또는 이메일이 다릅니다");
        }
        // Contact identifiers are exact strings, never locale-formatted numbers/dates (e.g. ?version=1.5).
        var sourceDates = TranslationDates.extract(CONTACT.matcher(sourceText).replaceAll(" "), request.from());
        var targetDates = TranslationDates.extract(CONTACT.matcher(targetText).replaceAll(" "), request.to());
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
        if (!sourceNumbers.counts().equals(targetNumbers.counts())) {
            reasons.add("원문과 번역의 숫자 값 또는 출현 횟수가 다릅니다");
        }
        return reasons.isEmpty() ? Verdict.ok() : new Verdict(false, reasons);
    }

    // ASCII URI characters stop before Korean/Japanese punctuation. Preserve paths, query and case exactly.
    private static final java.util.regex.Pattern CONTACT = java.util.regex.Pattern.compile(
            "(?i:https?)://[a-zA-Z0-9%._~:/?#\\[\\]@!$&'()*+,;=+-]+|[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static java.util.Map<String, Long> contacts(String text) {
        return CONTACT.matcher(text).results().map(m -> m.group().replaceAll("[.,;:!?)]+$", ""))
                .collect(java.util.stream.Collectors.groupingBy(java.util.function.Function.identity(), java.util.stream.Collectors.counting()));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
