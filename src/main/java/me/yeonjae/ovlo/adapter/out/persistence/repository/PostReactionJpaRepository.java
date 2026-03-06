package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.PostReactionJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.entity.PostReactionJpaEntity.PostReactionId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostReactionJpaRepository extends JpaRepository<PostReactionJpaEntity, PostReactionId> {
    List<PostReactionJpaEntity> findByIdPostId(Long postId);
    void deleteByIdPostId(Long postId);
}
