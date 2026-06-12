package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 학교 이메일 인증 코드 발송 요청. universityId = 인증 대상 대학(본교/파견 무관). */
public record RequestEmailVerificationRequest(
        @NotNull Long universityId,
        @NotBlank @Email String schoolEmail
) {}
