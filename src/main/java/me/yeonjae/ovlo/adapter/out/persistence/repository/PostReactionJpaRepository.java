package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.PostReactionJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.entity.PostReactionJpaEntity.PostReactionId;
import me.yeonjae.ovlo.domain.post.model.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PostReactionJpaRepository extends JpaRepository<PostReactionJpaEntity, PostReactionId> {
    List<PostReactionJpaEntity> findByIdPostId(Long postId);
    List<PostReactionJpaEntity> findByIdPostIdIn(java.util.Collection<Long> postIds);

    // Assigned composite IDs make save() use merge. Always INSERT here so a competing
    // insertion cannot silently become an UPDATE followed by a second count increment.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO post_reaction (post_id, member_id, reaction_type, created_at)
            VALUES (:postId, :memberId, :type, CURRENT_TIMESTAMP)
            """, nativeQuery = true)
    void insertReaction(@Param("postId") Long postId, @Param("memberId") Long memberId,
                        @Param("type") String type);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE PostReactionJpaEntity r SET r.reactionType = :next
            WHERE r.id.postId = :postId AND r.id.memberId = :memberId AND r.reactionType = :previous
            """)
    int changeTypeIfCurrent(@Param("postId") Long postId, @Param("memberId") Long memberId,
                            @Param("previous") ReactionType previous, @Param("next") ReactionType next);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM PostReactionJpaEntity r
            WHERE r.id.postId = :postId AND r.id.memberId = :memberId AND r.reactionType = :previous
            """)
    int deleteIfCurrent(@Param("postId") Long postId, @Param("memberId") Long memberId,
                        @Param("previous") ReactionType previous);
}
