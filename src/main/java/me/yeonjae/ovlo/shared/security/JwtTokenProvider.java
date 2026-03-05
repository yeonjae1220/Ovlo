package me.yeonjae.ovlo.shared.security;

import me.yeonjae.ovlo.domain.member.model.MemberId;

public interface JwtTokenProvider {
    String generateAccessToken(MemberId memberId);
    String generateRefreshToken();
    MemberId extractMemberId(String accessToken);
    boolean validateAccessToken(String accessToken);
}
