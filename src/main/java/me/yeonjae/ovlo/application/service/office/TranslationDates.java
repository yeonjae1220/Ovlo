package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.domain.office.model.NoticeLang;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Recognizes unambiguous notice dates only; never guesses a missing year or an ambiguous numeric order. */
final class TranslationDates {
    private TranslationDates() {}

    record DateValue(Integer year, int month, int day) {
        DateValue {
            // A yearless February 29 is valid; whether it falls in a leap year is not supplied.
            LocalDate.of(year == null ? 2000 : year, month, day);
        }
    }

    record Extracted(Map<DateValue, Integer> dates, String remainingText, boolean invalidDate) {}

    static Extracted extract(String text, NoticeLang language) {
        StringBuilder remaining = new StringBuilder(text);
        Map<DateValue, Integer> dates = new HashMap<>();
        boolean invalid = collect(remaining, dates,
                "(?<![\\p{L}\\d])(?<y>\\d{4})[-./](?<m>\\d{1,2})[-./](?<d>\\d{1,2})(?!\\d)",
                m -> value(m, Integer.parseInt(m.group("m"))));
        invalid |= collect(remaining, dates,
                "(?<!\\d)(?:(?<y>\\d{4})\\s*[년年]\\s*)?(?<m>\\d{1,2})\\s*[월月]\\s*(?<d>\\d{1,2})\\s*[일日]",
                m -> value(m, Integer.parseInt(m.group("m"))));
        if (language == NoticeLang.DE || language == NoticeLang.FR) {
            invalid |= collect(remaining, dates,
                    "(?<![\\p{L}\\d])(?<d>\\d{1,2})[./](?<m>\\d{1,2})[./](?<y>\\d{4})(?!\\d)",
                    m -> value(m, Integer.parseInt(m.group("m"))));
        }
        if (language == NoticeLang.VI) {
            invalid |= collect(remaining, dates,
                    "(?:ngày\\s+)?(?<d>\\d{1,2})\\s+tháng\\s+(?<m>\\d{1,2})(?:\\s+năm\\s+(?<y>\\d{4}))?(?!\\d)",
                    m -> value(m, Integer.parseInt(m.group("m"))));
        }
        if (language == NoticeLang.EN || language == NoticeLang.DE || language == NoticeLang.FR) {
            Locale locale = Locale.forLanguageTag(language.code());
            Map<String, Integer> months = new HashMap<>();
            for (Month month : Month.values()) {
                for (TextStyle style : new TextStyle[]{TextStyle.FULL, TextStyle.SHORT}) {
                    months.put(month.getDisplayName(style, locale).replace(".", "").toLowerCase(locale), month.getValue());
                }
            }
            String monthNames = months.keySet().stream().sorted(java.util.Comparator.comparingInt(String::length).reversed())
                    .map(Pattern::quote).collect(java.util.stream.Collectors.joining("|"));
            String month = "(?<month>" + monthNames + ")\\.?(?!\\p{L})";
            String day = "(?<d>\\d{1,2})(?:st|nd|rd|th|er)?\\.?";
            String year = "(?:[\\s,]+(?<y>\\d{4})(?!\\d))?";
            Function<Matcher, DateValue> parse = m -> value(m, months.get(m.group("month").toLowerCase(locale)));
            invalid |= collect(remaining, dates, "(?<![\\p{L}\\d])" + day + "\\s+" + month + year, parse);
            if (language == NoticeLang.EN) {
                invalid |= collect(remaining, dates, "(?<![\\p{L}\\d])" + month + "\\s+" + day + year + "(?!\\d)", parse);
            }
        }
        return new Extracted(Map.copyOf(dates), remaining.toString(), invalid);
    }

    private static DateValue value(Matcher match, int month) {
        String year = match.group("y");
        return new DateValue(year == null ? null : Integer.valueOf(year), month, Integer.parseInt(match.group("d")));
    }

    private static boolean collect(StringBuilder remaining, Map<DateValue, Integer> dates, String regex,
                                   Function<Matcher, DateValue> parse) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(remaining.toString());
        boolean invalid = false;
        while (matcher.find()) {
            try {
                dates.merge(parse.apply(matcher), 1, Integer::sum);
                // Keep date digits out of the amount/duration check: a date must not satisfy a missing price.
                for (int i = matcher.start(); i < matcher.end(); i++) remaining.setCharAt(i, ' ');
            } catch (DateTimeException e) {
                invalid = true;
            }
        }
        return invalid;
    }
}
