package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.domain.auth.exception.AuthException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Apple identity token 의 nonce 클레임은 클라이언트 원본 nonce 의 SHA-256 hex 여야 한다.
 * 가로챈 토큰에는 해시만 들어 있어 원본을 만들 수 없으므로 토큰 재사용을 막는다.
 */
final class AppleNonce {

    private static final String FAILURE = "Apple 인증을 확인할 수 없습니다. 다시 시도해 주세요";

    private AppleNonce() {}

    static void requireMatching(String rawNonce, String tokenNonce) {
        if (rawNonce == null || rawNonce.isBlank() || tokenNonce == null) {
            throw new AuthException(FAILURE);
        }
        byte[] expected = sha256Hex(rawNonce).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, tokenNonce.getBytes(StandardCharsets.UTF_8))) {
            throw new AuthException(FAILURE);
        }
    }

    private static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 쓸 수 없습니다", e);
        }
    }
}
