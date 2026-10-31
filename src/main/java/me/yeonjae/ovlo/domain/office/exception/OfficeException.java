package me.yeonjae.ovlo.domain.office.exception;

/**
 * 국제처(Office) 도메인 예외. ErrorType → HTTP 매핑은 GlobalExceptionHandler 가 한다.
 * NOT_FOUND/CONFLICT/FORBIDDEN/BAD_REQUEST 는 이름 그대로, SUSPENDED 는 403 으로 간다.
 */
public class OfficeException extends RuntimeException {

    public enum ErrorType {
        NOT_FOUND,
        CONFLICT,
        FORBIDDEN,   // 소속이 아니거나 범위 밖
        BAD_REQUEST, // 마지막 관리자 보호 등 상태 규칙 위반
        SUSPENDED    // 정지된 Office
    }

    private final ErrorType errorType;

    public OfficeException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
