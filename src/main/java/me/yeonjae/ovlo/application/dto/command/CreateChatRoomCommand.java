package me.yeonjae.ovlo.application.dto.command;

import me.yeonjae.ovlo.domain.chat.model.ChatRoomType;

import java.util.List;

public record CreateChatRoomCommand(
        ChatRoomType type,
        String name,
        List<Long> participantIds
) {
}
