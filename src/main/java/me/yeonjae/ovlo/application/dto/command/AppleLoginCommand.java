package me.yeonjae.ovlo.application.dto.command;

/**
 * @param identityToken Apple 이 발급한 identity token(JWT)
 * @param rawNonce      클라이언트가 만든 원본 nonce. 토큰에는 이 값의 SHA-256 hex 가 들어 있어야 한다.
 * @param name          Apple 이 첫 인증 때만 클라이언트에 주는 이름. 없으면 null.
 */
public record AppleLoginCommand(String identityToken, String rawNonce, String name) {}
