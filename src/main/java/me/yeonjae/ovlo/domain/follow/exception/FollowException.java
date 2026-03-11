package me.yeonjae.ovlo.domain.follow.exception;

public class FollowException extends RuntimeException {

    public enum ErrorType { CONFLICT, NOT_FOUND }

    private final ErrorType errorType;

    public FollowException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public FollowException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() { return errorType; }
}
