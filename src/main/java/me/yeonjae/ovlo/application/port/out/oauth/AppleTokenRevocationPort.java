package me.yeonjae.ovlo.application.port.out.oauth;

import me.yeonjae.ovlo.application.dto.result.AppleTokens;

/**
 * Apple 계정 연결 해제(Sign in with Apple 토큰 폐기). 탈퇴할 때 Apple 이 요구한다(App Store 5.1.1(v)).
 * 실패하면 AuthException — code 가 거절되면 UNAUTHORIZED, Apple 에 닿지 못하면 UNAVAILABLE.
 */
public interface AppleTokenRevocationPort {

    enum TokenType { REFRESH_TOKEN, ACCESS_TOKEN }

    /** @param redirectUri 웹 인증에서 받은 code 면 그때의 redirect URI, 앱에서 받은 code 면 null */
    AppleTokens exchange(String authorizationCode, String clientId, String redirectUri);

    void revoke(String clientId, String token, TokenType type);
}
