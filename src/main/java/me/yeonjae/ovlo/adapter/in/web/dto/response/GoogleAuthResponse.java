package me.yeonjae.ovlo.adapter.in.web.dto.response;

public record GoogleAuthResponse(String accessToken, Long memberId, boolean newMember) {
}
