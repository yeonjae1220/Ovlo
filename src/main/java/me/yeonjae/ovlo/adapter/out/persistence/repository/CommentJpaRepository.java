package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.CommentJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentJpaRepository extends JpaRepository<CommentJpaEntity, Long> {
    List<CommentJpaEntity> findByPostIdAndHiddenByWithdrawalFalse(Long postId);
    List<CommentJpaEntity> findByPostIdAndDeletedFalseAndHiddenByWithdrawalFalse(Long postId, Pageable pageable);
    long countByPostIdAndDeletedFalseAndHiddenByWithdrawalFalse(Long postId);

    @Modifying
    @Query("UPDATE CommentJpaEntity c SET c.hiddenByWithdrawal = true WHERE c.authorId = :authorId")
    void hideAllByAuthorId(@Param("authorId") Long authorId);
}
