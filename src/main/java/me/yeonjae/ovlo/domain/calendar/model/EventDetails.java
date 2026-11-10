package me.yeonjae.ovlo.domain.calendar.model;

import java.time.Instant;

public record EventDetails(String title, String description, EventCategory category, EventPeriod period,
                           String location, String actionUrl, Instant deadlineAt, String audience) {
    public EventDetails {
        if (title == null || title.isBlank() || title.length() > 200)
            throw new IllegalArgumentException("제목은 1~200자로 입력해 주세요");
        title = title.trim();
        if (category == null || period == null) throw new IllegalArgumentException("분류와 기간은 필수입니다");
        checkLength(description, 10000); checkLength(location, 300); checkLength(audience, 500);
        EventSource.validateUrl(actionUrl, false);
    }
    private static void checkLength(String value, int max) {
        if (value != null && value.length() > max) throw new IllegalArgumentException("입력 길이를 초과했습니다: " + max);
    }
}
