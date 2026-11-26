package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param identityToken Apple identity token(JWT)
 * @param nonce         클라이언트가 만든 원본 nonce. Apple 에는 이 값의 SHA-256 hex 를 보냈어야 한다.
 * @param name          Apple 이 첫 인증 때만 주는 이름(선택)
 */
public record AppleLoginRequest(
        @NotBlank @Size(max = 8192) String identityToken,
        @NotBlank @Size(min = 16, max = 128) String nonce,
        @Size(max = 200) String name
) {}
