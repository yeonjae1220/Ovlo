package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterMemberRequest(
        @NotBlank String name,
        @NotBlank String hometown,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다") String password,
        @NotNull Long homeUniversityId,
        @NotBlank String majorName,
        @NotBlank String degreeType,
        @Min(1) @Max(6) int gradeLevel
) {}
