package me.yeonjae.ovlo.shared.exception;

import java.util.Map;

public record ErrorResponse(String error, Map<String, String> fieldErrors) {

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, null);
    }

    public static ErrorResponse ofFields(Map<String, String> fieldErrors) {
        return new ErrorResponse("요청 값이 올바르지 않습니다.", fieldErrors);
    }
}
