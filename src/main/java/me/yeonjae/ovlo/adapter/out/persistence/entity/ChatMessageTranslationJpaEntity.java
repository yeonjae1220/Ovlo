package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "chat_message_translation",
        uniqueConstraints = @UniqueConstraint(name = "uq_chat_message_translation", columnNames = {"message_id", "lang"}))
public class ChatMessageTranslationJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false) private Long messageId;
    @Column(nullable = false, length = 5) private String lang;
    @Column(nullable = false, columnDefinition = "TEXT") private String text;
    @Column(name = "model_id", length = 100) private String modelId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ChatMessageTranslationJpaEntity() {}

    public Long getId() { return id; }
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = lang; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
}
