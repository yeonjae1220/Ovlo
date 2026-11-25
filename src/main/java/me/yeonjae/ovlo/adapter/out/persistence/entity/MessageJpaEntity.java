package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "message")
// 신고 처리로 내린 메시지는 어떤 JPA 조회에도 나오지 않는다(네이티브 SQL 은 각자 조건을 건다).
@org.hibernate.annotations.SQLRestriction("hidden_by_moderation = false")
public class MessageJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "chat_room_id", nullable = false) private Long chatRoomId;
    @Column(name = "sender_id", nullable = false) private Long senderId;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(name = "sent_at", nullable = false) private Instant sentAt;
    @Column(name = "hidden_by_withdrawal", nullable = false) private boolean hiddenByWithdrawal;
    public MessageJpaEntity() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getChatRoomId() { return chatRoomId; } public void setChatRoomId(Long chatRoomId) { this.chatRoomId = chatRoomId; }
    public Long getSenderId() { return senderId; } public void setSenderId(Long senderId) { this.senderId = senderId; }
    public String getContent() { return content; } public void setContent(String content) { this.content = content; }
    public Instant getSentAt() { return sentAt; } public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public boolean isHiddenByWithdrawal() { return hiddenByWithdrawal; } public void setHiddenByWithdrawal(boolean hiddenByWithdrawal) { this.hiddenByWithdrawal = hiddenByWithdrawal; }
}
