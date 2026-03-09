package me.yeonjae.ovlo.application.port.in.chat;

import me.yeonjae.ovlo.application.dto.result.ChatRoomResult;
import me.yeonjae.ovlo.application.dto.result.MessageResult;

import java.util.List;

public interface GetChatRoomQuery {

    ChatRoomResult getChatRoom(Long chatRoomId);

    List<ChatRoomResult> getChatRooms(Long memberId);

    List<MessageResult> getMessages(Long chatRoomId, int page, int size);

    boolean isMemberOfRoom(Long chatRoomId, Long memberId);
}
