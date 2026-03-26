package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompleteOnboardingRequest(
        @NotBlank String hometown,
        @NotNull Long homeUniversityId,
        @NotBlank String majorName,
        @NotBlank String degreeType,
        @Min(1) @Max(10) int gradeLevel
) {}
