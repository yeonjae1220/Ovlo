package me.yeonjae.ovlo.adapter.in.web.dto.response;

import me.yeonjae.ovlo.domain.board.model.Board;

public record AdminBoardResponse(
        Long id,
        String name,
        String category,
        String scope,
        Long creatorId,
        Long universityId,
        boolean active
) {
    public static AdminBoardResponse of(Board board) {
        return new AdminBoardResponse(
                board.getId() != null ? board.getId().value() : null,
                board.getName(),
                board.getCategory().name(),
                board.getScope().name(),
                board.getCreatorId().value(),
                board.getUniversityId() != null ? board.getUniversityId().value() : null,
                board.isActive()
        );
    }
}
