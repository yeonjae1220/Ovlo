package me.yeonjae.ovlo.application.dto.command;

public record ConfirmSchoolEmailVerificationCommand(
        Long memberId,
        String code
) {}
