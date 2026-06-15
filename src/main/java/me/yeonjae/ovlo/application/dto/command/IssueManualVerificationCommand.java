package me.yeonjae.ovlo.application.dto.command;

/**
 * 관리자 수동 대학 인증 발급 커맨드.
 * 수동 인증은 증빙서류 기반이므로 학교 이메일을 받지 않는다(필요 시 note에 기록).
 *
 * @param memberId     인증 대상 회원
 * @param universityId 인증 대상 대학
 * @param note         발급 사유 메모(선택)
 * @param adminEmail   발급한 관리자 식별자(감사용)
 */
public record IssueManualVerificationCommand(
        Long memberId,
        Long universityId,
        String note,
        String adminEmail
) {}
