package me.yeonjae.ovlo.domain.office.exception;

public class PartnerException extends RuntimeException {

    public enum ErrorType {
        NOT_FOUND,
        BAD_REQUEST,
        FORBIDDEN   // 그 대학에 꺼져 있거나 플랫폼에서 비활성
    }

    private final ErrorType errorType;

    public PartnerException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
