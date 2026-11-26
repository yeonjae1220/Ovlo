package me.yeonjae.ovlo.application.dto.result;

/**
 * 서명·발급자·audience·만료를 검증한 Apple identity token 의 내용.
 *
 * @param subject Apple 사용자 식별자(sub). 같은 팀의 앱·웹 사이에서 변하지 않는다.
 * @param email   이메일. 사용자가 숨기면 privaterelay 주소이고, 요청 범위에 email 이 없으면 null.
 * @param nonce   토큰에 담긴 nonce. 클라이언트는 원본 nonce 의 SHA-256 hex 를 Apple 에 보낸다.
 */
public record AppleIdentity(String subject, String email, String nonce) {}
