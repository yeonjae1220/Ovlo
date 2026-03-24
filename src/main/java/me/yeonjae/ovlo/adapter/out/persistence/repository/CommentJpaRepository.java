package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.CommentJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentJpaRepository extends JpaRepository<CommentJpaEntity, Long> {
    List<CommentJpaEntity> findByPostId(Long postId);
    List<CommentJpaEntity> findByPostIdAndDeletedFalse(Long postId, Pageable pageable);
    long countByPostIdAndDeletedFalse(Long postId);
}
