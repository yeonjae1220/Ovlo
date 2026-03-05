package me.yeonjae.ovlo.domain.auth.model;

import java.util.Objects;
import java.util.UUID;

public record AuthSessionId(String value) {

    public AuthSessionId {
        Objects.requireNonNull(value, "AuthSessionId는 null일 수 없습니다");
        if (value.isBlank()) {
            throw new IllegalArgumentException("AuthSessionId는 빈 값일 수 없습니다");
        }
    }

    public static AuthSessionId generate() {
        return new AuthSessionId(UUID.randomUUID().toString());
    }
}
