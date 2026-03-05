package me.yeonjae.ovlo.application.port.in.board;

import me.yeonjae.ovlo.application.dto.command.SubscribeBoardCommand;

public interface SubscribeBoardUseCase {
    void subscribe(SubscribeBoardCommand command);
}
