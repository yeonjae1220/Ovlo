package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.port.out.board.LoadBoardPort;
import me.yeonjae.ovlo.application.port.out.board.SaveBoardPort;
import me.yeonjae.ovlo.application.port.out.board.SearchBoardPort;
import me.yeonjae.ovlo.domain.board.model.*;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Board JPA 구현 예정 (stub).
 * LoadBoardPort / SaveBoardPort / SearchBoardPort 구현.
 */
@Component
public class BoardPersistenceAdapter implements LoadBoardPort, SaveBoardPort, SearchBoardPort {

    @Override
    public Optional<Board> findById(BoardId boardId) {
        throw new UnsupportedOperationException("Board JPA 구현 예정");
    }

    @Override
    public boolean existsSubscription(BoardId boardId, MemberId memberId) {
        throw new UnsupportedOperationException("Board JPA 구현 예정");
    }

    @Override
    public Optional<BoardSubscription> findSubscription(BoardId boardId, MemberId memberId) {
        throw new UnsupportedOperationException("Board JPA 구현 예정");
    }

    @Override
    public Board save(Board board) {
        throw new UnsupportedOperationException("Board JPA 구현 예정");
    }

    @Override
    public BoardSubscription saveSubscription(BoardSubscription subscription) {
        throw new UnsupportedOperationException("Board JPA 구현 예정");
    }

    @Override
    public void deleteSubscription(BoardSubscription subscription) {
        throw new UnsupportedOperationException("Board JPA 구현 예정");
    }

    @Override
    public List<Board> search(String keyword, BoardCategory category, LocationScope scope, int offset, int limit) {
        throw new UnsupportedOperationException("Board JPA 구현 예정");
    }

    @Override
    public long count(String keyword, BoardCategory category, LocationScope scope) {
        throw new UnsupportedOperationException("Board JPA 구현 예정");
    }
}
