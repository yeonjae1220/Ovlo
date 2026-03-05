package me.yeonjae.ovlo.domain.member.model;

import java.util.Objects;

public record ContactInfo(ContactType type, String value) {

    public ContactInfo {
        Objects.requireNonNull(type, "연락처 타입은 null일 수 없습니다");
        Objects.requireNonNull(value, "연락처 값은 null일 수 없습니다");
        if (value.isBlank()) {
            throw new IllegalArgumentException("연락처 값은 빈 값일 수 없습니다");
        }
    }
}
