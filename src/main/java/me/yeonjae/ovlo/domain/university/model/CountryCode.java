package me.yeonjae.ovlo.domain.university.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record CountryCode(String value) {

    private static final Pattern ALPHA2_PATTERN = Pattern.compile("[A-Z]{2}");

    public CountryCode {
        Objects.requireNonNull(value, "국가 코드는 null일 수 없습니다");
        value = value.toUpperCase();
        if (!ALPHA2_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "국가 코드는 ISO 3166-1 alpha-2 형식(대문자 2글자)이어야 합니다: " + value);
        }
    }
}
