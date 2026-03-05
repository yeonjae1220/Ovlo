package me.yeonjae.ovlo.domain.auth.model;

import me.yeonjae.ovlo.domain.member.model.MemberId;

import java.time.Instant;
import java.util.Objects;

public class AuthSession {

    private AuthSessionId id;
    private MemberId memberId;
    private String refreshToken;
    private Instant expiresAt;
    private boolean revoked;

    private AuthSession() {}

    public static AuthSession create(MemberId memberId, String refreshToken, Instant expiresAt) {
        Objects.requireNonNull(memberId, "memberId는 null일 수 없습니다");
        Objects.requireNonNull(refreshToken, "리프레시 토큰은 null일 수 없습니다");
        if (refreshToken.isBlank()) {
            throw new IllegalArgumentException("리프레시 토큰은 빈 값일 수 없습니다");
        }
        Objects.requireNonNull(expiresAt, "만료 시간은 null일 수 없습니다");
        if (!expiresAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("만료 시간은 현재 이후여야 합니다");
        }

        AuthSession session = new AuthSession();
        session.id = AuthSessionId.generate();
        session.memberId = memberId;
        session.refreshToken = refreshToken;
        session.expiresAt = expiresAt;
        session.revoked = false;
        return session;
    }

    // persistence 계층 전용 — 저장소에서 복원할 때 사용
    public static AuthSession restore(AuthSessionId id, MemberId memberId,
                                      String refreshToken, Instant expiresAt, boolean revoked) {
        Objects.requireNonNull(id, "id는 null일 수 없습니다");
        Objects.requireNonNull(memberId, "memberId는 null일 수 없습니다");
        Objects.requireNonNull(refreshToken, "refreshToken은 null일 수 없습니다");
        Objects.requireNonNull(expiresAt, "expiresAt은 null일 수 없습니다");

        AuthSession session = new AuthSession();
        session.id = id;
        session.memberId = memberId;
        session.refreshToken = refreshToken;
        session.expiresAt = expiresAt;
        session.revoked = revoked;
        return session;
    }

    // ── 도메인 행위 ──────────────────────────────────────────────────────────

    public boolean isExpired() {
        return revoked || !expiresAt.isAfter(Instant.now());
    }

    public void revoke() {
        if (this.revoked) {
            throw new IllegalStateException("이미 무효화된 세션입니다");
        }
        this.revoked = true;
    }

    public void rotate(String newToken, Instant newExpiry) {
        Objects.requireNonNull(newToken, "새 리프레시 토큰은 null일 수 없습니다");
        Objects.requireNonNull(newExpiry, "새 만료 시간은 null일 수 없습니다");
        if (isExpired()) {
            throw new IllegalStateException("유효하지 않은 세션입니다. 재로그인이 필요합니다");
        }
        if (!newExpiry.isAfter(Instant.now())) {
            throw new IllegalArgumentException("만료 시간은 현재 이후여야 합니다");
        }
        this.refreshToken = newToken;
        this.expiresAt = newExpiry;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public AuthSessionId getId() { return id; }
    public MemberId getMemberId() { return memberId; }
    public String getRefreshToken() { return refreshToken; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }
}
