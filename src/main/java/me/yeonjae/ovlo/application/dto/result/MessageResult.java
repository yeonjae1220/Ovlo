package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.chat.model.Message;

import java.time.LocalDateTime;

public record MessageResult(
        Long messageId,
        Long senderId,
        String content,
        LocalDateTime sentAt
) {
    public static MessageResult from(Message message) {
        return new MessageResult(
                message.getId() != null ? message.getId().value() : null,
                message.getSenderId().value(),
                message.getContent(),
                message.getSentAt()
        );
    }
}
