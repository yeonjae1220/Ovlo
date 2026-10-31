package me.yeonjae.ovlo.domain.office.exception;

/** 상담 이관(Consultation) 예외. CONSENT_REQUIRED 는 400, INVALID_STATE 는 409. */
public class ConsultationException extends RuntimeException {

    public enum ErrorType {
        NOT_FOUND,
        FORBIDDEN,         // 당사자(학생·그 Office 담당자)가 아님
        BAD_REQUEST,       // 활동 담당자 없음 등
        CONSENT_REQUIRED,  // 동의 없음·알 수 없는 버전
        INVALID_STATE      // 허용되지 않는 상태 전이
    }

    private final ErrorType errorType;

    public ConsultationException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
