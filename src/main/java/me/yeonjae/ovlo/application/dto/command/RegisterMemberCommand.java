package me.yeonjae.ovlo.application.dto.command;

public record RegisterMemberCommand(
        String nickname,
        String name,
        String hometown,
        String email,
        String rawPassword,
        Long homeUniversityId,
        String majorName,
        String degreeType,
        Integer gradeLevel
) {}
