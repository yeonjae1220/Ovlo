package me.yeonjae.ovlo.domain.auth.exception;

public class AuthException extends RuntimeException {

    /** UNAVAILABLE: 외부 인증 제공자(Apple 등)에 닿지 못해 지금은 처리할 수 없음(503, 재시도 가능). */
    public enum ErrorType { UNAUTHORIZED, CONFLICT, FORBIDDEN, BAD_REQUEST, UNAVAILABLE }

    private final ErrorType errorType;

    public AuthException(String message) {
        this(ErrorType.UNAUTHORIZED, message);
    }

    public AuthException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = ErrorType.UNAUTHORIZED;
    }

    public ErrorType getErrorType() { return errorType; }
}
