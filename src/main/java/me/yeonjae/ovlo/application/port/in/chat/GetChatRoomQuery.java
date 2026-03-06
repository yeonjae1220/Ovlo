package me.yeonjae.ovlo.application.port.in.chat;

import me.yeonjae.ovlo.application.dto.result.ChatRoomResult;

import java.util.List;

public interface GetChatRoomQuery {

    ChatRoomResult getChatRoom(Long chatRoomId);

    List<ChatRoomResult> getChatRooms(Long memberId);
}
