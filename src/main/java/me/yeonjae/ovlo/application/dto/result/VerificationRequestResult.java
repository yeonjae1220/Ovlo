package me.yeonjae.ovlo.application.dto.result;

/** 코드 발송 결과 (코드 자체는 응답에 절대 포함하지 않는다). */
public record VerificationRequestResult(
        String maskedEmail,
        long expiresInSeconds
) {}
