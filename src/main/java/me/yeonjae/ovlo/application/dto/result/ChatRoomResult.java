package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.chat.model.ChatRoom;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomType;

import java.util.List;

public record ChatRoomResult(
        Long chatRoomId,
        ChatRoomType type,
        String name,
        List<Long> participantIds
) {
    public static ChatRoomResult from(ChatRoom room) {
        return new ChatRoomResult(
                room.getId() != null ? room.getId().value() : null,
                room.getType(),
                room.getName(),
                room.getParticipants().stream()
                        .map(p -> p.value())
                        .toList()
        );
    }
}
