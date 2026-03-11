package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.CreateBoardCommand;
import me.yeonjae.ovlo.application.dto.command.SubscribeBoardCommand;
import me.yeonjae.ovlo.application.dto.result.BoardResult;
import me.yeonjae.ovlo.application.port.in.board.CreateBoardUseCase;
import me.yeonjae.ovlo.application.port.in.board.SubscribeBoardUseCase;
import me.yeonjae.ovlo.application.port.in.board.UnsubscribeBoardUseCase;
import me.yeonjae.ovlo.application.port.out.board.LoadBoardPort;
import me.yeonjae.ovlo.application.port.out.board.SaveBoardPort;
import me.yeonjae.ovlo.domain.board.exception.BoardException;
import me.yeonjae.ovlo.domain.board.model.*;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BoardCommandService implements
        CreateBoardUseCase, SubscribeBoardUseCase, UnsubscribeBoardUseCase {

    private final LoadBoardPort loadBoardPort;
    private final SaveBoardPort saveBoardPort;

    public BoardCommandService(LoadBoardPort loadBoardPort, SaveBoardPort saveBoardPort) {
        this.loadBoardPort = loadBoardPort;
        this.saveBoardPort = saveBoardPort;
    }

    @Override
    public BoardResult create(CreateBoardCommand command) {
        BoardCategory category = BoardCategory.valueOf(command.category());
        LocationScope scope = LocationScope.valueOf(command.scope());
        UniversityId universityId = command.universityId() != null
                ? new UniversityId(command.universityId()) : null;

        Board board = Board.create(
                command.name(),
                command.description(),
                category,
                scope,
                new MemberId(command.creatorId()),
                universityId);

        Board saved = saveBoardPort.save(board);
        return BoardResult.from(saved);
    }

    @Override
    public void subscribe(SubscribeBoardCommand command) {
        BoardId boardId = new BoardId(command.boardId());
        MemberId memberId = new MemberId(command.memberId());

        loadBoardPort.findById(boardId)
                .orElseThrow(() -> new BoardException("게시판을 찾을 수 없습니다: " + command.boardId(), BoardException.ErrorType.NOT_FOUND));

        if (loadBoardPort.existsSubscription(boardId, memberId)) {
            throw new BoardException("이미 구독 중인 게시판입니다", BoardException.ErrorType.CONFLICT);
        }

        BoardSubscription subscription = BoardSubscription.create(boardId, memberId);
        try {
            saveBoardPort.saveSubscription(subscription);
        } catch (DataIntegrityViolationException e) {
            throw new BoardException("이미 구독 중인 게시판입니다", BoardException.ErrorType.CONFLICT);
        }
    }

    @Override
    public void unsubscribe(SubscribeBoardCommand command) {
        BoardId boardId = new BoardId(command.boardId());
        MemberId memberId = new MemberId(command.memberId());

        BoardSubscription subscription = loadBoardPort.findSubscription(boardId, memberId)
                .orElseThrow(() -> new BoardException("구독 정보를 찾을 수 없습니다", BoardException.ErrorType.NOT_FOUND));

        saveBoardPort.deleteSubscription(subscription);
    }
}
