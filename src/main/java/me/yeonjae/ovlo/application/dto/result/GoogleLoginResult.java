package me.yeonjae.ovlo.application.dto.result;

public record GoogleLoginResult(
        String accessToken,
        String refreshToken,
        Long memberId,
        boolean newMember
) {}
