package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "chat_room_read_marker")
public class ChatRoomReadMarkerJpaEntity {

    @EmbeddedId
    private ReadMarkerId id;

    @Column(name = "last_read_at", nullable = false)
    private LocalDateTime lastReadAt;

    public ChatRoomReadMarkerJpaEntity() {}

    public ChatRoomReadMarkerJpaEntity(Long chatRoomId, Long memberId) {
        this.id = new ReadMarkerId(chatRoomId, memberId);
        this.lastReadAt = LocalDateTime.now();
    }

    public ReadMarkerId getId() { return id; }
    public LocalDateTime getLastReadAt() { return lastReadAt; }
    public void setLastReadAt(LocalDateTime lastReadAt) { this.lastReadAt = lastReadAt; }

    @Embeddable
    public static class ReadMarkerId implements Serializable {

        @Column(name = "chat_room_id")
        private Long chatRoomId;

        @Column(name = "member_id")
        private Long memberId;

        public ReadMarkerId() {}

        public ReadMarkerId(Long chatRoomId, Long memberId) {
            this.chatRoomId = chatRoomId;
            this.memberId = memberId;
        }

        public Long getChatRoomId() { return chatRoomId; }
        public Long getMemberId() { return memberId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ReadMarkerId r)) return false;
            return Objects.equals(chatRoomId, r.chatRoomId) && Objects.equals(memberId, r.memberId);
        }

        @Override
        public int hashCode() { return Objects.hash(chatRoomId, memberId); }
    }
}
