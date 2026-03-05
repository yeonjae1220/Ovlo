package me.yeonjae.ovlo.application.port.in.board;

import me.yeonjae.ovlo.application.dto.command.SearchBoardCommand;
import me.yeonjae.ovlo.application.dto.result.BoardPageResult;

public interface SearchBoardQuery {
    BoardPageResult search(SearchBoardCommand command);
}
