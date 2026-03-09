package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.chat.model.ChatRoom;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomType;

import java.util.List;
import java.util.Map;

public record ChatRoomResult(
        Long chatRoomId,
        ChatRoomType type,
        String name,
        List<Long> participantIds,
        Map<Long, String> participantNicknames,
        Map<Long, String> participantProfileImageMediaIds
) {
    public static ChatRoomResult from(ChatRoom room) {
        return from(room, Map.of(), Map.of());
    }

    public static ChatRoomResult from(ChatRoom room, Map<Long, String> nicknames, Map<Long, String> profileImages) {
        return new ChatRoomResult(
                room.getId() != null ? room.getId().value() : null,
                room.getType(),
                room.getName(),
                room.getParticipants().stream().map(p -> p.value()).toList(),
                nicknames,
                profileImages
        );
    }
}
