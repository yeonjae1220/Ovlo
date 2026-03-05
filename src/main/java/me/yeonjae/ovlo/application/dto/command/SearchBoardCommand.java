package me.yeonjae.ovlo.application.dto.command;

public record SearchBoardCommand(
        String keyword,
        String category,
        String scope,
        int page,
        int size
) {
    public int offset() {
        return page * size;
    }
}
