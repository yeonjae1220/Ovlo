package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.PostJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostJpaRepository extends JpaRepository<PostJpaEntity, Long> {
    List<PostJpaEntity> findByBoardIdAndDeletedFalse(Long boardId, Pageable pageable);
    long countByBoardIdAndDeletedFalse(Long boardId);
}
