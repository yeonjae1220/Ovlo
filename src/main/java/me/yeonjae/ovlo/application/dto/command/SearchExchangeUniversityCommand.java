package me.yeonjae.ovlo.application.dto.command;

public record SearchExchangeUniversityCommand(
        String keyword,
        String country,
        int page,
        int size
) {}
