package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import me.yeonjae.ovlo.domain.post.model.ReactionType;
import org.hibernate.annotations.CreationTimestamp;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "post_reaction")
public class PostReactionJpaEntity {

    @Embeddable
    public static class PostReactionId implements Serializable {
        @Column(name = "post_id") private Long postId;
        @Column(name = "member_id") private Long memberId;
        public PostReactionId() {}
        public PostReactionId(Long postId, Long memberId) { this.postId = postId; this.memberId = memberId; }
        public Long getPostId() { return postId; } public void setPostId(Long postId) { this.postId = postId; }
        public Long getMemberId() { return memberId; } public void setMemberId(Long memberId) { this.memberId = memberId; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof PostReactionId that)) return false; return Objects.equals(postId, that.postId) && Objects.equals(memberId, that.memberId); }
        @Override public int hashCode() { return Objects.hash(postId, memberId); }
    }

    @EmbeddedId private PostReactionId id;
    @Column(name = "reaction_type", nullable = false) @Enumerated(EnumType.STRING) private ReactionType reactionType;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public PostReactionJpaEntity() {}
    public PostReactionJpaEntity(Long postId, Long memberId, ReactionType reactionType) {
        this.id = new PostReactionId(postId, memberId);
        this.reactionType = reactionType;
    }
    public PostReactionId getId() { return id; } public void setId(PostReactionId id) { this.id = id; }
    public Long getPostId() { return id != null ? id.getPostId() : null; }
    public Long getMemberId() { return id != null ? id.getMemberId() : null; }
    public ReactionType getReactionType() { return reactionType; } public void setReactionType(ReactionType reactionType) { this.reactionType = reactionType; }
}
