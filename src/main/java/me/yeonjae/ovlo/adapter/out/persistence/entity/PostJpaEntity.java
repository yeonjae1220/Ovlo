package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "post")
public class PostJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "board_id", nullable = false) private Long boardId;
    @Column(name = "author_id", nullable = false) private Long authorId;
    @Column(nullable = false) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(nullable = false) private boolean deleted;
    @Version private Long version;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    public PostJpaEntity() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getBoardId() { return boardId; } public void setBoardId(Long boardId) { this.boardId = boardId; }
    public Long getAuthorId() { return authorId; } public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public String getTitle() { return title; } public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; } public void setContent(String content) { this.content = content; }
    public boolean isDeleted() { return deleted; } public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Long getVersion() { return version; } public void setVersion(Long version) { this.version = version; }
}
