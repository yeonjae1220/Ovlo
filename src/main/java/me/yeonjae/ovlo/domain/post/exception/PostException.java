package me.yeonjae.ovlo.domain.post.exception;

public class PostException extends RuntimeException {

    public enum ErrorType { NOT_FOUND, CONFLICT, FORBIDDEN, BAD_REQUEST }

    private final ErrorType errorType;

    public PostException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() { return errorType; }
}
