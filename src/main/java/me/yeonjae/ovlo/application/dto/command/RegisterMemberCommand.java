package me.yeonjae.ovlo.application.dto.command;

public record RegisterMemberCommand(
        String name,
        String hometown,
        String email,
        String rawPassword,
        Long homeUniversityId,
        String majorName,
        String degreeType,
        int gradeLevel
) {}
