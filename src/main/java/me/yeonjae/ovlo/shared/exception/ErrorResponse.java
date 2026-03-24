package me.yeonjae.ovlo.shared.exception;

import java.util.Map;

public record ErrorResponse(String code, String error, Map<String, String> fieldErrors) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null);
    }

    public static ErrorResponse ofFields(String code, Map<String, String> fieldErrors) {
        return new ErrorResponse(code, "요청 값이 올바르지 않습니다.", fieldErrors);
    }
}
