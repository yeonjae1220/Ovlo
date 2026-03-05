package me.yeonjae.ovlo.domain.university.model;

import java.util.Objects;

public record UniversityId(Long value) {
    public UniversityId {
        Objects.requireNonNull(value, "UniversityId는 null일 수 없습니다");
    }
}
