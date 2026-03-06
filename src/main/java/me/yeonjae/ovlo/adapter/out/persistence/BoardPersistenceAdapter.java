package me.yeonjae.ovlo.adapter.out.persistence;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import me.yeonjae.ovlo.adapter.out.persistence.entity.QBoardJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.mapper.BoardMapper;
import me.yeonjae.ovlo.adapter.out.persistence.repository.BoardJpaRepository;
import me.yeonjae.ovlo.adapter.out.persistence.repository.BoardSubscriptionJpaRepository;
import me.yeonjae.ovlo.application.port.out.board.LoadBoardPort;
import me.yeonjae.ovlo.application.port.out.board.SaveBoardPort;
import me.yeonjae.ovlo.application.port.out.board.SearchBoardPort;
import me.yeonjae.ovlo.domain.board.model.*;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class BoardPersistenceAdapter implements LoadBoardPort, SaveBoardPort, SearchBoardPort {

    private final BoardJpaRepository boardJpaRepository;
    private final BoardSubscriptionJpaRepository boardSubscriptionJpaRepository;
    private final BoardMapper boardMapper;
    private final JPAQueryFactory queryFactory;

    public BoardPersistenceAdapter(BoardJpaRepository boardJpaRepository,
                                   BoardSubscriptionJpaRepository boardSubscriptionJpaRepository,
                                   BoardMapper boardMapper,
                                   JPAQueryFactory queryFactory) {
        this.boardJpaRepository = boardJpaRepository;
        this.boardSubscriptionJpaRepository = boardSubscriptionJpaRepository;
        this.boardMapper = boardMapper;
        this.queryFactory = queryFactory;
    }

    @Override
    public Optional<Board> findById(BoardId boardId) {
        return boardJpaRepository.findById(boardId.value()).map(boardMapper::toDomain);
    }

    @Override
    public boolean existsSubscription(BoardId boardId, MemberId memberId) {
        return boardSubscriptionJpaRepository.existsByIdBoardIdAndIdMemberId(boardId.value(), memberId.value());
    }

    @Override
    public Optional<BoardSubscription> findSubscription(BoardId boardId, MemberId memberId) {
        return boardSubscriptionJpaRepository.findByIdBoardIdAndIdMemberId(boardId.value(), memberId.value())
                .map(boardMapper::toSubscriptionDomain);
    }

    @Override
    public Board save(Board board) {
        return boardMapper.toDomain(boardJpaRepository.save(boardMapper.toJpaEntity(board)));
    }

    @Override
    public BoardSubscription saveSubscription(BoardSubscription subscription) {
        boardSubscriptionJpaRepository.save(boardMapper.toSubscriptionJpaEntity(subscription));
        return subscription;
    }

    @Override
    public void deleteSubscription(BoardSubscription subscription) {
        boardSubscriptionJpaRepository.findByIdBoardIdAndIdMemberId(
                subscription.getBoardId().value(), subscription.getMemberId().value())
                .ifPresent(boardSubscriptionJpaRepository::delete);
    }

    @Override
    public List<Board> search(String keyword, BoardCategory category, LocationScope scope, int offset, int limit) {
        QBoardJpaEntity q = QBoardJpaEntity.boardJpaEntity;
        return queryFactory.selectFrom(q)
                .where(buildPredicate(q, keyword, category, scope))
                .orderBy(q.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch()
                .stream()
                .map(boardMapper::toDomain)
                .toList();
    }

    @Override
    public long count(String keyword, BoardCategory category, LocationScope scope) {
        QBoardJpaEntity q = QBoardJpaEntity.boardJpaEntity;
        Long result = queryFactory.select(q.count()).from(q).where(buildPredicate(q, keyword, category, scope)).fetchOne();
        return result != null ? result : 0L;
    }

    private BooleanBuilder buildPredicate(QBoardJpaEntity q, String keyword, BoardCategory category, LocationScope scope) {
        BooleanBuilder predicate = new BooleanBuilder();
        if (keyword != null && !keyword.isBlank()) {
            predicate.and(q.name.containsIgnoreCase(keyword));
        }
        if (category != null) {
            predicate.and(q.category.eq(category));
        }
        if (scope != null) {
            predicate.and(q.scope.eq(scope));
        }
        return predicate;
    }
}
