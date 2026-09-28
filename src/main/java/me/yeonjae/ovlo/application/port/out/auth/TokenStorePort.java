package me.yeonjae.ovlo.application.port.out.auth;

import me.yeonjae.ovlo.domain.auth.model.AuthSession;
import me.yeonjae.ovlo.domain.auth.model.RefreshRotationOutcome;
import me.yeonjae.ovlo.domain.member.model.MemberId;

import java.util.Optional;

public interface TokenStorePort {
    /** 새 세션을 저장한다(로그인). 기존 세션의 토큰 교체는 {@link #rotate}를 쓴다. */
    void save(AuthSession session);

    /**
     * {@code presentedToken}이 세션의 현재 토큰일 때만 {@code rotated}의 새 토큰·만료로 교체한다.
     * 비교와 교체는 한 원자 구간에서 일어나므로, 같은 토큰으로 시작한 동시 요청 중 하나만 ROTATED를 받는다.
     */
    RefreshRotationOutcome rotate(AuthSession rotated, String presentedToken);

    Optional<AuthSession> findByMemberId(MemberId memberId);
    Optional<AuthSession> findByRefreshToken(String refreshToken);
    void delete(MemberId memberId);
    void deleteByRefreshToken(String refreshToken);
}
