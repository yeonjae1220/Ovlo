package me.yeonjae.ovlo.application.port.in.board;

import me.yeonjae.ovlo.application.dto.command.SearchBoardCommand;
import me.yeonjae.ovlo.application.dto.result.BoardResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;

public interface SearchBoardQuery {
    PageResult<BoardResult> search(SearchBoardCommand command);
}
