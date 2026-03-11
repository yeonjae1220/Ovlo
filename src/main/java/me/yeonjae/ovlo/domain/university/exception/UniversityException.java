package me.yeonjae.ovlo.domain.university.exception;

public class UniversityException extends RuntimeException {

    public enum ErrorType { NOT_FOUND }

    private final ErrorType errorType;

    public UniversityException(String message) {
        super(message);
        this.errorType = ErrorType.NOT_FOUND;
    }

    public UniversityException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = ErrorType.NOT_FOUND;
    }

    public ErrorType getErrorType() { return errorType; }
}
