package me.yeonjae.ovlo.application.port.out.chat;

import me.yeonjae.ovlo.domain.chat.model.ChatRoom;
import me.yeonjae.ovlo.domain.chat.model.Message;

public interface SaveChatPort {

    ChatRoom save(ChatRoom chatRoom);

    /** 채팅방 전체 로딩 없이 메시지 단건 INSERT 후 도메인 객체 반환 */
    Message saveMessage(Long chatRoomId, Long senderId, String content);
}
