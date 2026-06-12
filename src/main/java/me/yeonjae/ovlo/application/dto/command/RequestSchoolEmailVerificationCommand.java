package me.yeonjae.ovlo.application.dto.command;

/**
 * 학교 이메일 인증 코드 발송 요청.
 * universityId: 인증 대상 대학(본교/파견 무관, 사용자가 카탈로그에서 지정).
 */
public record RequestSchoolEmailVerificationCommand(
        Long memberId,
        Long universityId,
        String schoolEmail
) {}
