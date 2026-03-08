package me.yeonjae.ovlo.application.dto.command;

public record UpdateMemberProfileCommand(
        Long memberId,
        String nickname,
        String bio
) {}
