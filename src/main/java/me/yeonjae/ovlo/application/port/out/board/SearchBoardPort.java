package me.yeonjae.ovlo.application.port.out.board;

import me.yeonjae.ovlo.domain.board.model.Board;
import me.yeonjae.ovlo.domain.board.model.BoardCategory;
import me.yeonjae.ovlo.domain.board.model.LocationScope;

import java.util.List;

public interface SearchBoardPort {
    List<Board> search(String keyword, BoardCategory category, LocationScope scope, int offset, int limit);
    long count(String keyword, BoardCategory category, LocationScope scope);
}
