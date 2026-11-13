package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.domain.office.model.NoticeLang;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Locale-specific numeric values, without floating-point rounding or guessing ambiguous separators. */
final class TranslationNumbers {
    private TranslationNumbers() {}

    record Extracted(Map<BigDecimal, Integer> counts, boolean invalidNumber) {}

    private static final Pattern NUMBER = Pattern.compile(
            "(?:(?<![\\p{L}\\d])[+\\-−])?(?:[0-9]+(?:[.,][0-9]+)*|[.,][0-9]+)");
    private static final Pattern FRENCH_NUMBER = Pattern.compile(
            "(?:(?<![\\p{L}\\d])[+\\-−])?(?:[0-9]+(?:[.,][0-9]+|[ \\u00a0\\u202f][0-9]{3}(?![0-9]))*|[.,][0-9]+)");

    static Extracted extract(String text, NoticeLang language) {
        boolean commaDecimal = language == NoticeLang.DE || language == NoticeLang.FR || language == NoticeLang.VI;
        String decimal = commaDecimal ? "," : ".";
        String grouping = language == NoticeLang.FR ? " " : commaDecimal ? "." : ",";
        String integer = "(?:[0-9]+|[0-9]{1,3}(?:" + Pattern.quote(grouping) + "[0-9]{3})+)";
        Pattern valid = Pattern.compile("[+-]?(?:" + integer + "(?:" + Pattern.quote(decimal)
                + "[0-9]+)?|" + Pattern.quote(decimal) + "[0-9]+)");
        var matcher = (language == NoticeLang.FR ? FRENCH_NUMBER : NUMBER).matcher(text);
        Map<BigDecimal, Integer> counts = new HashMap<>();
        boolean invalid = false;
        while (matcher.find()) {
            String token = matcher.group().replace('−', '-').replace('\u00a0', ' ').replace('\u202f', ' ');
            if (!valid.matcher(token).matches()) {
                invalid = true;
                continue;
            }
            String normalized = token.replace(grouping, "").replace(decimal, ".");
            BigDecimal value = new BigDecimal(normalized).stripTrailingZeros();
            counts.merge(value, 1, Integer::sum);
        }
        return new Extracted(Map.copyOf(counts), invalid);
    }
}
