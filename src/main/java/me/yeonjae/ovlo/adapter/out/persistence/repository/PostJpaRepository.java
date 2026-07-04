package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.PostJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostJpaRepository extends JpaRepository<PostJpaEntity, Long> {
    List<PostJpaEntity> findByBoardIdAndDeletedFalseAndHiddenByWithdrawalFalse(Long boardId, Pageable pageable);
    long countByBoardIdAndDeletedFalseAndHiddenByWithdrawalFalse(Long boardId);
    List<PostJpaEntity> findAllByDeletedFalseAndHiddenByWithdrawalFalse(Pageable pageable);
    long countByDeletedFalseAndHiddenByWithdrawalFalse();
    List<PostJpaEntity> findByAuthorIdAndDeletedFalseAndHiddenByWithdrawalFalse(Long authorId, Pageable pageable);
    long countByAuthorIdAndDeletedFalseAndHiddenByWithdrawalFalse(Long authorId);

    @Modifying
    @Query("UPDATE PostJpaEntity p SET p.hiddenByWithdrawal = true WHERE p.authorId = :authorId")
    void hideAllByAuthorId(@Param("authorId") Long authorId);

    /**
     * 좋아요 카운트를 원자적으로 증감한다(A안).
     *
     * <p>{@code like_count = like_count + :delta}는 DB가 최신 커밋 값을 읽어 델타를 적용하므로
     * 동시 반응이 낙관적 락/재시도 없이도 lost update 없이 정확히 수렴한다. 벌크 UPDATE라
     * 영속성 컨텍스트를 우회하므로, 앞선 반응 행 변경을 먼저 flush 하고 이후 stale 엔티티를 비운다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PostJpaEntity p SET p.likeCount = p.likeCount + :delta WHERE p.id = :postId")
    void addLikeCount(@Param("postId") Long postId, @Param("delta") long delta);

    /** 싫어요 카운트를 원자적으로 증감한다(A안). {@link #addLikeCount} 참고. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PostJpaEntity p SET p.dislikeCount = p.dislikeCount + :delta WHERE p.id = :postId")
    void addDislikeCount(@Param("postId") Long postId, @Param("delta") long delta);
}
