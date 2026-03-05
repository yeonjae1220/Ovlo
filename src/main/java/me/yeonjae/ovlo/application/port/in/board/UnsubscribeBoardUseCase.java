package me.yeonjae.ovlo.application.port.in.board;

import me.yeonjae.ovlo.application.dto.command.SubscribeBoardCommand;

public interface UnsubscribeBoardUseCase {
    void unsubscribe(SubscribeBoardCommand command);
}
