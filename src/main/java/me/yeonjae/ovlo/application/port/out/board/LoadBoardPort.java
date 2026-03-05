package me.yeonjae.ovlo.application.port.out.board;

import me.yeonjae.ovlo.domain.board.model.Board;
import me.yeonjae.ovlo.domain.board.model.BoardId;
import me.yeonjae.ovlo.domain.board.model.BoardSubscription;
import me.yeonjae.ovlo.domain.member.model.MemberId;

import java.util.Optional;

public interface LoadBoardPort {
    Optional<Board> findById(BoardId boardId);
    boolean existsSubscription(BoardId boardId, MemberId memberId);
    Optional<BoardSubscription> findSubscription(BoardId boardId, MemberId memberId);
}
