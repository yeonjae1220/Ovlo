package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.Pattern;

public record ConfirmEmailVerificationRequest(
        @Pattern(regexp = "\\d{6}", message = "인증 코드는 6자리 숫자여야 합니다") String code
) {}
