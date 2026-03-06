package me.yeonjae.ovlo.adapter.out.persistence.mapper;

import me.yeonjae.ovlo.adapter.out.persistence.entity.BoardJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.entity.BoardSubscriptionJpaEntity;
import me.yeonjae.ovlo.domain.board.model.*;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.springframework.stereotype.Component;

@Component
public class BoardMapper {

    public BoardJpaEntity toJpaEntity(Board board) {
        BoardJpaEntity entity = new BoardJpaEntity();
        if (board.getId() != null) {
            entity.setId(board.getId().value());
        }
        entity.setName(board.getName());
        entity.setDescription(board.getDescription());
        entity.setCategory(board.getCategory());
        entity.setScope(board.getScope());
        entity.setCreatorId(board.getCreatorId().value());
        entity.setUniversityId(board.getUniversityId() != null ? board.getUniversityId().value() : null);
        entity.setActive(board.isActive());
        return entity;
    }

    public Board toDomain(BoardJpaEntity entity) {
        return Board.restore(
                new BoardId(entity.getId()),
                entity.getName(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getScope(),
                new MemberId(entity.getCreatorId()),
                entity.getUniversityId() != null ? new UniversityId(entity.getUniversityId()) : null,
                entity.isActive()
        );
    }

    public BoardSubscriptionJpaEntity toSubscriptionJpaEntity(BoardSubscription subscription) {
        return new BoardSubscriptionJpaEntity(
                subscription.getBoardId().value(),
                subscription.getMemberId().value()
        );
    }

    public BoardSubscription toSubscriptionDomain(BoardSubscriptionJpaEntity entity) {
        return BoardSubscription.create(
                new BoardId(entity.getBoardId()),
                new MemberId(entity.getMemberId())
        );
    }
}
