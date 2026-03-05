package me.yeonjae.ovlo.application.port.in.chat;

import me.yeonjae.ovlo.application.dto.result.ChatRoomResult;

public interface GetChatRoomQuery {

    ChatRoomResult getChatRoom(Long chatRoomId);
}
