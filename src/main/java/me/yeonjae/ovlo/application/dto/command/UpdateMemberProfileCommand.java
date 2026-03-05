package me.yeonjae.ovlo.application.dto.command;

public record UpdateMemberProfileCommand(
        Long memberId,
        String name,
        String hometown,
        String majorName,
        String degreeType,
        Integer gradeLevel
) {}
