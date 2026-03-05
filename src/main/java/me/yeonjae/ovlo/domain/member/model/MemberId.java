package me.yeonjae.ovlo.domain.member.model;

import java.util.Objects;

public record MemberId(Long value) {
    public MemberId {
        Objects.requireNonNull(value, "MemberId는 null일 수 없습니다");
    }
}
