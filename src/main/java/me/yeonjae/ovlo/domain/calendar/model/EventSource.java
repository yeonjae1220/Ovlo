package me.yeonjae.ovlo.domain.calendar.model;

import java.net.URI;
import java.time.LocalDate;

public record EventSource(String url, LocalDate checkedOn) {
    public EventSource {
        validateUrl(url, true);
        if (checkedOn == null || checkedOn.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("출처 확인일은 오늘 또는 이전 날짜여야 합니다");
    }
    public static void validateUrl(String url, boolean required) {
        if (url == null || url.isBlank()) {
            if (required) throw new IllegalArgumentException("공식 출처 링크가 필요합니다");
            return;
        }
        try {
            URI uri = URI.create(url);
            if (url.length() > 2000 || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null)
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("올바른 HTTP(S) 링크를 입력해 주세요");
        }
    }
}
