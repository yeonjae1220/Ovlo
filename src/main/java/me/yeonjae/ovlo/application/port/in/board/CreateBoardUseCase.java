package me.yeonjae.ovlo.application.port.in.board;

import me.yeonjae.ovlo.application.dto.command.CreateBoardCommand;
import me.yeonjae.ovlo.application.dto.result.BoardResult;

public interface CreateBoardUseCase {
    BoardResult create(CreateBoardCommand command);
}
