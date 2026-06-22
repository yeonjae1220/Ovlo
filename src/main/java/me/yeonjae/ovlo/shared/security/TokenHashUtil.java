package me.yeonjae.ovlo.shared.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * 고엔트로피 토큰(예: refresh token)을 Redis 등 저장소에 두기 전 단방향 해시한다.
 *
 * <p>refresh token 자체가 충분히 무작위(고엔트로피)이므로 bcrypt/argon2 같은 느린
 * KDF가 아니라 SHA-256 단방향 해시면 충분하다. 저장소(Redis)가 유출되더라도 원문
 * 토큰을 복원할 수 없어 세션 탈취를 막는다.
 *
 * <p>salt를 쓰지 않는 이유: 토큰을 해시값으로 조회(역인덱스)해야 하므로 동일 입력은
 * 동일 출력이어야 한다. 토큰이 고엔트로피라 레인보우 테이블/사전 공격이 무의미하다.
 */
public final class TokenHashUtil {

    private TokenHashUtil() {
    }

    /**
     * 토큰을 SHA-256으로 해시해 소문자 hex 문자열(64자)로 반환한다.
     *
     * @param token 원문 토큰 (null 금지)
     * @return SHA-256 hex 다이제스트
     */
    public static String sha256(String token) {
        Objects.requireNonNull(token, "토큰은 null일 수 없습니다");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 모든 JVM이 보장하는 표준 알고리즘이라 도달 불가
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다", e);
        }
    }
}
