package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.CommentJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentJpaRepository extends JpaRepository<CommentJpaEntity, Long> {
    interface CommentCount { Long getPostId(); Long getTotal(); }

    @Query("SELECT c.postId AS postId, COUNT(c) AS total FROM CommentJpaEntity c WHERE c.postId IN :ids AND c.deleted=false AND c.hiddenByWithdrawal=false GROUP BY c.postId")
    List<CommentCount> countVisibleByPostIds(@Param("ids") List<Long> ids);

    @Modifying
    @Query("UPDATE CommentJpaEntity c SET c.deleted=true WHERE c.id=:id AND c.postId=:postId AND c.authorId=:authorId AND c.deleted=false AND c.hiddenByWithdrawal=false")
    int deleteOwnedComment(@Param("postId") Long postId, @Param("id") Long id, @Param("authorId") Long authorId);

    List<CommentJpaEntity> findByPostIdAndHiddenByWithdrawalFalse(Long postId);
    List<CommentJpaEntity> findByPostIdAndDeletedFalseAndHiddenByWithdrawalFalse(Long postId, Pageable pageable);
    long countByPostIdAndDeletedFalseAndHiddenByWithdrawalFalse(Long postId);

    @Modifying
    @Query("UPDATE CommentJpaEntity c SET c.hiddenByWithdrawal = true WHERE c.authorId = :authorId")
    void hideAllByAuthorId(@Param("authorId") Long authorId);
}
