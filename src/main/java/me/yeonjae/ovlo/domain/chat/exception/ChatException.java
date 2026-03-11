package me.yeonjae.ovlo.domain.chat.exception;

public class ChatException extends RuntimeException {

    public enum ErrorType { NOT_FOUND, CONFLICT, BAD_REQUEST }

    private final ErrorType errorType;

    public ChatException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public ChatException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() { return errorType; }
}
