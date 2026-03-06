package me.yeonjae.ovlo.adapter.in.web.dto.request;

public record UpdateMemberProfileRequest(
        String name,
        String hometown,
        String majorName,
        String degreeType,
        Integer gradeLevel
) {}
