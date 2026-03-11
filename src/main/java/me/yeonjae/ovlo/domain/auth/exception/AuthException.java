package me.yeonjae.ovlo.domain.auth.exception;

public class AuthException extends RuntimeException {

    public enum ErrorType { UNAUTHORIZED }

    private final ErrorType errorType;

    public AuthException(String message) {
        super(message);
        this.errorType = ErrorType.UNAUTHORIZED;
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = ErrorType.UNAUTHORIZED;
    }

    public ErrorType getErrorType() { return errorType; }
}
