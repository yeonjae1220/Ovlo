package me.yeonjae.ovlo.domain.member.model;

import java.util.Objects;

public record Password(String hashedValue) {

    public Password {
        Objects.requireNonNull(hashedValue, "비밀번호는 null일 수 없습니다");
        if (hashedValue.isBlank()) {
            throw new IllegalArgumentException("비밀번호는 빈 값일 수 없습니다");
        }
    }
}
