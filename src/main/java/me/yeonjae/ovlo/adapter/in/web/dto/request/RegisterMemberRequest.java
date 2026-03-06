package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterMemberRequest(
        @NotBlank String name,
        @NotBlank String hometown,
        @NotBlank String email,
        @NotBlank String password,
        @NotNull Long homeUniversityId,
        @NotBlank String majorName,
        @NotBlank String degreeType,
        int gradeLevel
) {}
