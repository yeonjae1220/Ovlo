package me.yeonjae.ovlo.application.port.out.board;

import me.yeonjae.ovlo.domain.board.model.Board;
import me.yeonjae.ovlo.domain.board.model.BoardSubscription;

public interface SaveBoardPort {
    Board save(Board board);
    BoardSubscription saveSubscription(BoardSubscription subscription);
    void deleteSubscription(BoardSubscription subscription);
}
