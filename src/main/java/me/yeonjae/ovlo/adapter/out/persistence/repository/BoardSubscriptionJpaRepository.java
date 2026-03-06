package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.BoardSubscriptionJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.entity.BoardSubscriptionJpaEntity.BoardSubscriptionId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BoardSubscriptionJpaRepository extends JpaRepository<BoardSubscriptionJpaEntity, BoardSubscriptionId> {
    Optional<BoardSubscriptionJpaEntity> findByIdBoardIdAndIdMemberId(Long boardId, Long memberId);
    boolean existsByIdBoardIdAndIdMemberId(Long boardId, Long memberId);
}
