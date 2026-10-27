package me.yeonjae.ovlo.shared.security;

import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;

import java.time.Duration;

public interface JwtTokenProvider {
    String generateAccessToken(MemberId memberId, MemberRole role);
    String generateRefreshToken();

    /**
     * refresh 토큰(=세션) 수명. {@code jwt.refresh-token-ttl-minutes} 한 곳에서 오고, refresh 쿠키
     * maxAge 도 같은 키를 쓴다 — 둘이 어긋나면 쿠키만 남은 유령 세션이 된다(GLOBAL-PIT-070).
     */
    Duration refreshTokenTtl();
    MemberId extractMemberId(String accessToken);
    MemberRole extractRole(String accessToken);
    boolean validateAccessToken(String accessToken);
}
