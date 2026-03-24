package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.command.SearchBoardCommand;
import me.yeonjae.ovlo.application.dto.result.BoardResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.port.in.board.GetBoardQuery;
import me.yeonjae.ovlo.application.port.in.board.SearchBoardQuery;
import me.yeonjae.ovlo.application.port.out.board.LoadBoardPort;
import me.yeonjae.ovlo.application.port.out.board.SearchBoardPort;
import me.yeonjae.ovlo.domain.board.exception.BoardException;
import me.yeonjae.ovlo.domain.board.model.BoardCategory;
import me.yeonjae.ovlo.domain.board.model.BoardId;
import me.yeonjae.ovlo.domain.board.model.LocationScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BoardQueryService implements SearchBoardQuery, GetBoardQuery {

    private final LoadBoardPort loadBoardPort;
    private final SearchBoardPort searchBoardPort;

    public BoardQueryService(LoadBoardPort loadBoardPort, SearchBoardPort searchBoardPort) {
        this.loadBoardPort = loadBoardPort;
        this.searchBoardPort = searchBoardPort;
    }

    @Override
    public PageResult<BoardResult> search(SearchBoardCommand command) {
        BoardCategory category = command.category() != null
                ? BoardCategory.valueOf(command.category()) : null;
        LocationScope scope = command.scope() != null
                ? LocationScope.valueOf(command.scope()) : null;

        List<BoardResult> content = searchBoardPort
                .search(command.keyword(), category, scope, command.offset(), command.size())
                .stream()
                .map(BoardResult::from)
                .toList();

        long total = searchBoardPort.count(command.keyword(), category, scope);
        return PageResult.of(content, total, command.page(), command.size());
    }

    @Override
    public BoardResult getById(BoardId boardId) {
        return loadBoardPort.findById(boardId)
                .map(BoardResult::from)
                .orElseThrow(() -> new BoardException("게시판을 찾을 수 없습니다: " + boardId.value(), BoardException.ErrorType.NOT_FOUND));
    }
}
