package me.yeonjae.ovlo.domain.media.exception;

public class MediaException extends RuntimeException {

    public enum ErrorType { NOT_FOUND }

    private final ErrorType errorType;

    public MediaException(String message) {
        super(message);
        this.errorType = ErrorType.NOT_FOUND;
    }

    public MediaException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = ErrorType.NOT_FOUND;
    }

    public ErrorType getErrorType() { return errorType; }
}
