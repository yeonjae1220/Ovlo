package me.yeonjae.ovlo.domain.university.exception;

public class UniversityException extends RuntimeException {

    public UniversityException(String message) {
        super(message);
    }

    public UniversityException(String message, Throwable cause) {
        super(message, cause);
    }
}
