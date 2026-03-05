package me.yeonjae.ovlo.application.dto.command;

public record SendMessageCommand(
        Long chatRoomId,
        Long senderId,
        String content
) {
}
