package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "message")
public class MessageJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "chat_room_id", nullable = false) private Long chatRoomId;
    @Column(name = "sender_id", nullable = false) private Long senderId;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(name = "sent_at", nullable = false) private LocalDateTime sentAt;
    @Column(name = "hidden_by_withdrawal", nullable = false) private boolean hiddenByWithdrawal;
    public MessageJpaEntity() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getChatRoomId() { return chatRoomId; } public void setChatRoomId(Long chatRoomId) { this.chatRoomId = chatRoomId; }
    public Long getSenderId() { return senderId; } public void setSenderId(Long senderId) { this.senderId = senderId; }
    public String getContent() { return content; } public void setContent(String content) { this.content = content; }
    public LocalDateTime getSentAt() { return sentAt; } public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public boolean isHiddenByWithdrawal() { return hiddenByWithdrawal; } public void setHiddenByWithdrawal(boolean hiddenByWithdrawal) { this.hiddenByWithdrawal = hiddenByWithdrawal; }
}
