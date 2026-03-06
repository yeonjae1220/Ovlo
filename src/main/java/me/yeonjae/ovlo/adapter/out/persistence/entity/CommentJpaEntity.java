package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "comment")
public class CommentJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "post_id", nullable = false) private Long postId;
    @Column(name = "author_id", nullable = false) private Long authorId;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(nullable = false) private boolean deleted;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    public CommentJpaEntity() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getPostId() { return postId; } public void setPostId(Long postId) { this.postId = postId; }
    public Long getAuthorId() { return authorId; } public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public String getContent() { return content; } public void setContent(String content) { this.content = content; }
    public boolean isDeleted() { return deleted; } public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
