package me.yeonjae.ovlo.domain.auth.exception;

public class AuthException extends RuntimeException {

    public enum ErrorType { UNAUTHORIZED, CONFLICT }

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
