package me.yeonjae.ovlo.application.port.in.board;

import me.yeonjae.ovlo.application.dto.result.BoardResult;
import me.yeonjae.ovlo.domain.board.model.BoardId;

public interface GetBoardQuery {
    BoardResult getById(BoardId boardId);
}
