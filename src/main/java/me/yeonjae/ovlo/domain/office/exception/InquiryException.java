package me.yeonjae.ovlo.domain.office.exception;

/** 학생 문의(Inquiry) 예외. RATE_LIMITED 는 429, 나머지는 이름대로. */
public class InquiryException extends RuntimeException {

    public enum ErrorType {
        NOT_FOUND,
        FORBIDDEN,     // 다른 회원의 문의
        BAD_REQUEST,   // 질문 길이·형식
        RATE_LIMITED   // Office 일일 상한
    }

    private final ErrorType errorType;

    public InquiryException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
