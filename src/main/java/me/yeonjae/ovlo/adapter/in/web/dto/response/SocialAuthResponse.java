package me.yeonjae.ovlo.adapter.in.web.dto.response;

public record SocialAuthResponse(String accessToken, Long memberId, boolean newMember) {
}
