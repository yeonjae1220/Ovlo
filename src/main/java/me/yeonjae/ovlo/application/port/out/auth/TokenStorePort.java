package me.yeonjae.ovlo.application.port.out.auth;

import me.yeonjae.ovlo.domain.auth.model.AuthSession;
import me.yeonjae.ovlo.domain.member.model.MemberId;

import java.util.Optional;

public interface TokenStorePort {
    void save(AuthSession session);
    Optional<AuthSession> findByMemberId(MemberId memberId);
    Optional<AuthSession> findByRefreshToken(String refreshToken);
    void delete(MemberId memberId);
    void deleteByRefreshToken(String refreshToken);
}
