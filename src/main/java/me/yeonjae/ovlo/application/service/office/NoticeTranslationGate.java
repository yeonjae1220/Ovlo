package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.port.out.office.TranslationPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 공지 번역 간이 게이트(설계 §5.1). 리포트 재생성 게이트의 축소판:
 * - 비어 있지 않을 것
 * - 원문과 동일하지 않을 것(번역이 안 된 복사)
 * - 원문의 숫자(날짜·금액·기간)가 번역에 빠짐없이 남을 것 — 숫자 변형은 가장 위험한 오류다
 * 실패하면 그 언어 번역은 저장하지 않는다. "확인 못 한 내용은 쓰지 않는다"와 같은 원칙.
 */
@Component
public class NoticeTranslationGate {

    private static final Pattern NUMBER = Pattern.compile("\\d+(?:[.,]\\d+)*");

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
        Set<String> sourceNumbers = numbersOf(request.title() + "\n" + request.body());
        Set<String> targetNumbers = numbersOf(result.title() + "\n" + result.body());
        Set<String> missing = sourceNumbers.stream()
                .filter(n -> !targetNumbers.contains(n))
                .collect(Collectors.toCollection(java.util.TreeSet::new));
        if (!missing.isEmpty()) {
            reasons.add("원문 숫자가 번역에 없습니다: " + String.join(", ", missing));
        }
        return reasons.isEmpty() ? Verdict.ok() : new Verdict(false, reasons);
    }

    /** 구분 기호(쉼표·점)를 제거해 "1,000"·"1000" 을 같은 값으로 본다. */
    static Set<String> numbersOf(String text) {
        Set<String> out = new java.util.HashSet<>();
        Matcher m = NUMBER.matcher(text == null ? "" : text);
        while (m.find()) {
            out.add(m.group().replaceAll("[.,]", ""));
        }
        return out;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
