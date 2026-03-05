package me.yeonjae.ovlo.application.dto.command;

public record SearchUniversityCommand(
        String keyword,
        String countryCode,
        int page,
        int size
) {
    public int offset() {
        return page * size;
    }
}
