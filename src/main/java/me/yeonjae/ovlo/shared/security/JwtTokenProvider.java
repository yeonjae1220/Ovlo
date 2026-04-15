package me.yeonjae.ovlo.shared.security;

import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;

public interface JwtTokenProvider {
    String generateAccessToken(MemberId memberId, MemberRole role);
    String generateRefreshToken();
    MemberId extractMemberId(String accessToken);
    MemberRole extractRole(String accessToken);
    boolean validateAccessToken(String accessToken);
}
