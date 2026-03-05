package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.board.model.Board;

public record BoardResult(
        Long id,
        String name,
        String description,
        String category,
        String scope,
        Long creatorId,
        Long universityId,
        boolean active
) {
    public static BoardResult from(Board board) {
        return new BoardResult(
                board.getId() != null ? board.getId().value() : null,
                board.getName(),
                board.getDescription(),
                board.getCategory().name(),
                board.getScope().name(),
                board.getCreatorId().value(),
                board.getUniversityId() != null ? board.getUniversityId().value() : null,
                board.isActive()
        );
    }
}
