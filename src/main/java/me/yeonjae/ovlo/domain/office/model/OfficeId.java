package me.yeonjae.ovlo.domain.office.model;

import java.util.Objects;

/** 국제처(Office) 식별자. */
public record OfficeId(Long value) {
    public OfficeId {
        Objects.requireNonNull(value, "OfficeId는 null일 수 없습니다");
    }
}
