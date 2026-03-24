package me.yeonjae.ovlo.domain.university.exception;

public class UniversityException extends RuntimeException {

    public enum ErrorType { NOT_FOUND }

    private final ErrorType errorType;

    protected UniversityException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    protected UniversityException(String message, Throwable cause, ErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() { return errorType; }

    public static class NotFound extends UniversityException {
        public NotFound(Long id) {
            super("대학을 찾을 수 없습니다: " + id, ErrorType.NOT_FOUND);
        }
    }
}
