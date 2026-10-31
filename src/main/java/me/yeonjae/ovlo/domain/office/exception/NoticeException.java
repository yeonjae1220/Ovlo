package me.yeonjae.ovlo.domain.office.exception;

/** 승인 자료(Notice) 도메인 예외. INVALID_STATE 는 409, 나머지는 이름대로 매핑한다. */
public class NoticeException extends RuntimeException {

    public enum ErrorType {
        NOT_FOUND,
        BAD_REQUEST,   // 날짜·필수값 규칙 위반
        INVALID_STATE  // 허용되지 않는 상태 전이 (보관된 공지 수정, 승인된 공지 재승인 등)
    }

    private final ErrorType errorType;

    public NoticeException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
