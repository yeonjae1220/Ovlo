package me.yeonjae.ovlo.domain.auth.model;

import java.util.Objects;

public record TokenPair(String accessToken, String refreshToken) {

    public TokenPair {
        Objects.requireNonNull(accessToken, "액세스 토큰은 null일 수 없습니다");
        if (accessToken.isBlank()) {
            throw new IllegalArgumentException("액세스 토큰은 빈 값일 수 없습니다");
        }
        Objects.requireNonNull(refreshToken, "리프레시 토큰은 null일 수 없습니다");
        if (refreshToken.isBlank()) {
            throw new IllegalArgumentException("리프레시 토큰은 빈 값일 수 없습니다");
        }
    }
}
