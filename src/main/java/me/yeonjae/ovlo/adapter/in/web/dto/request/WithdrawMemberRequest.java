package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 회원 탈퇴 본문. Apple 로그인 회원만 apple 을 채운다(탈퇴 직전 Apple 재인증 결과). */
public record WithdrawMemberRequest(@Valid AppleReauthRequest apple) {

    /**
     * @param redirectUri 웹 팝업 인증이면 그때의 redirect URI, 앱(네이티브)이면 비운다
     */
    public record AppleReauthRequest(
            @NotBlank @Size(max = 8192) String identityToken,
            @NotBlank @Size(min = 16, max = 128) String nonce,
            @NotBlank @Size(max = 1024) String authorizationCode,
            @Size(max = 512) String redirectUri
    ) {}
}
