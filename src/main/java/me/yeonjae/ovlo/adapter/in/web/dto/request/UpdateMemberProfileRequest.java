package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMemberProfileRequest(
        @Size(min = 2, max = 30, message = "닉네임은 2~30자 이내여야 합니다")
        @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "닉네임은 영문, 숫자, ., _ 만 사용 가능합니다")
        String nickname,

        @Size(max = 500, message = "소개는 500자를 초과할 수 없습니다")
        String bio
) {}
