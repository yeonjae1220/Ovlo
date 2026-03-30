package me.yeonjae.ovlo.application.dto.command;

public record SearchGlobalUniversityCommand(
        String keyword,
        String countryCode,
        int page,
        int size
) {}
