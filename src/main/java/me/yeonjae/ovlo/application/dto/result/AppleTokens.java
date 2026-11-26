package me.yeonjae.ovlo.application.dto.result;

/**
 * Apple 토큰 엔드포인트 교환 결과.
 *
 * @param subject      토큰 주인의 Apple 사용자 식별자(id_token 의 sub)
 * @param refreshToken 없을 수 있다
 * @param accessToken  없을 수 있다
 */
public record AppleTokens(String subject, String refreshToken, String accessToken) {}
