package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomType;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_room")
public class ChatRoomJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) @Enumerated(EnumType.STRING) private ChatRoomType type;
    private String name;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "chat_room_participant", joinColumns = @JoinColumn(name = "chat_room_id"))
    @Column(name = "member_id")
    private List<Long> participantIds = new ArrayList<>();
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    public ChatRoomJpaEntity() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public ChatRoomType getType() { return type; } public void setType(ChatRoomType type) { this.type = type; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public List<Long> getParticipantIds() { return participantIds; } public void setParticipantIds(List<Long> participantIds) { this.participantIds = participantIds; }
}
