package me.yeonjae.ovlo.application.dto.command;

/**
 * 탈퇴 직전 Apple 재인증 결과. 받은 authorization code 를 교환해 얻은 토큰을 Apple 에서 폐기한다.
 *
 * @param identityToken     같은 인증에서 받은 identity token — 탈퇴하려는 회원의 Apple ID 인지 확인한다
 * @param rawNonce          원본 nonce(토큰에는 SHA-256 hex 가 들어 있다)
 * @param authorizationCode 5분 안에 한 번 쓸 수 있는 code
 * @param redirectUri       웹 인증이면 그때 쓴 redirect URI, 앱(네이티브)이면 null
 */
public record AppleReauthorization(String identityToken, String rawNonce, String authorizationCode, String redirectUri) {}
