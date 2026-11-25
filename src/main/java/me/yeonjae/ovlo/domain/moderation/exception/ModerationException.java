package me.yeonjae.ovlo.domain.moderation.exception;

public class ModerationException extends RuntimeException {

    public enum ErrorType { BAD_REQUEST, NOT_FOUND, CONFLICT }

    private final ErrorType errorType;

    public ModerationException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() { return errorType; }
}
