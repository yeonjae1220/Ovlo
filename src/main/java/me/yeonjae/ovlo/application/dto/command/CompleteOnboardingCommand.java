package me.yeonjae.ovlo.application.dto.command;

public record CompleteOnboardingCommand(
        Long memberId,
        String hometown,
        Long homeUniversityId,
        String majorName,
        String degreeType,
        int gradeLevel
) {}
