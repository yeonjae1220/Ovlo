package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "board_subscription")
public class BoardSubscriptionJpaEntity {

    @Embeddable
    public static class BoardSubscriptionId implements Serializable {
        @Column(name = "board_id") private Long boardId;
        @Column(name = "member_id") private Long memberId;
        public BoardSubscriptionId() {}
        public BoardSubscriptionId(Long boardId, Long memberId) { this.boardId = boardId; this.memberId = memberId; }
        public Long getBoardId() { return boardId; } public void setBoardId(Long boardId) { this.boardId = boardId; }
        public Long getMemberId() { return memberId; } public void setMemberId(Long memberId) { this.memberId = memberId; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof BoardSubscriptionId that)) return false; return Objects.equals(boardId, that.boardId) && Objects.equals(memberId, that.memberId); }
        @Override public int hashCode() { return Objects.hash(boardId, memberId); }
    }

    @EmbeddedId private BoardSubscriptionId id;
    @CreationTimestamp @Column(name = "subscribed_at", nullable = false, updatable = false) private Instant subscribedAt;

    public BoardSubscriptionJpaEntity() {}
    public BoardSubscriptionJpaEntity(Long boardId, Long memberId) { this.id = new BoardSubscriptionId(boardId, memberId); }
    public BoardSubscriptionId getId() { return id; } public void setId(BoardSubscriptionId id) { this.id = id; }
    public Long getBoardId() { return id != null ? id.getBoardId() : null; }
    public Long getMemberId() { return id != null ? id.getMemberId() : null; }
}
