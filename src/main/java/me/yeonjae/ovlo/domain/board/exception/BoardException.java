package me.yeonjae.ovlo.domain.board.exception;

public class BoardException extends RuntimeException {

    public enum ErrorType { NOT_FOUND, CONFLICT }

    private final ErrorType errorType;

    public BoardException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() { return errorType; }
}
