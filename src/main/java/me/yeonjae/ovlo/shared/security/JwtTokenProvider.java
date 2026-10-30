package me.yeonjae.ovlo.shared.security;

import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.domain.office.model.OfficeMembership;

import java.time.Duration;
import java.util.Optional;

public interface JwtTokenProvider {

    /** 담당자 소속 없이 발급(일반 회원). {@link #generateAccessToken(MemberId, MemberRole, OfficeMembership)} 의 축약. */
    default String generateAccessToken(MemberId memberId, MemberRole role) {
        return generateAccessToken(memberId, role, null);
    }

    /**
     * 접근 토큰 발급. {@code officeMembership} 이 있으면 클레임 {@code officeId}·{@code officeRole} 을 넣는다(ADR-0005).
     * null 이면 두 클레임을 생략한다.
     */
    String generateAccessToken(MemberId memberId, MemberRole role, OfficeMembership officeMembership);

    String generateRefreshToken();

    /**
     * refresh 토큰(=세션) 수명. {@code jwt.refresh-token-ttl-minutes} 한 곳에서 오고, refresh 쿠키
     * maxAge 도 같은 키를 쓴다 — 둘이 어긋나면 쿠키만 남은 유령 세션이 된다(GLOBAL-PIT-070).
     */
    Duration refreshTokenTtl();
    MemberId extractMemberId(String accessToken);
    MemberRole extractRole(String accessToken);

    /** 토큰의 담당자 소속 클레임. 없거나 해석할 수 없으면 empty. */
    Optional<OfficeMembership> extractOfficeMembership(String accessToken);

    boolean validateAccessToken(String accessToken);
}
