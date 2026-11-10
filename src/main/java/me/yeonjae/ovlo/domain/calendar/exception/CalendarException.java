package me.yeonjae.ovlo.domain.calendar.exception;
public class CalendarException extends RuntimeException {
    public enum ErrorType { BAD_REQUEST, FORBIDDEN, NOT_FOUND, CONFLICT, UNAUTHORIZED }
    private final ErrorType errorType;
    public CalendarException(String message, ErrorType errorType) { super(message); this.errorType = errorType; }
    public ErrorType getErrorType() { return errorType; }
}
