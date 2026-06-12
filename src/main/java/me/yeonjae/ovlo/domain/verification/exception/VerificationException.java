package me.yeonjae.ovlo.domain.verification.exception;

/**
 * 인증 흐름 도메인 예외. ErrorType → HTTP 매핑은 GlobalExceptionHandler에서 처리.
 */
public class VerificationException extends RuntimeException {

    public enum ErrorType {
        DOMAIN_MISMATCH,      // 이메일 도메인이 지정한 대학과 일치하지 않음
        UNIVERSITY_NOT_RESOLVED, // 도메인이 카탈로그의 어떤 대학과도 매칭되지 않음
        PUBLIC_PROVIDER,      // 공개 메일(gmail 등) — 학교 메일 아님
        CHALLENGE_NOT_FOUND,  // 진행 중인 인증 챌린지 없음/만료
        CODE_MISMATCH,        // 코드 불일치
        CODE_EXPIRED,         // 코드 만료
        TOO_MANY_ATTEMPTS,    // 시도횟수 초과
        EMAIL_ALREADY_USED,   // 다른 멤버가 이미 인증한 학교 이메일
        RATE_LIMITED          // 발송 쿨다운/캡 초과
    }

    private final ErrorType errorType;

    public VerificationException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
