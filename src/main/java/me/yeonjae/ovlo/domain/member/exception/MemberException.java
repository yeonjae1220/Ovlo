package me.yeonjae.ovlo.domain.member.exception;

public class MemberException extends RuntimeException {

    public enum ErrorType { NOT_FOUND, CONFLICT }

    private final ErrorType errorType;

    public MemberException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public MemberException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() { return errorType; }
}
