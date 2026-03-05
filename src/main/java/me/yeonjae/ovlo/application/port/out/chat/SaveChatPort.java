package me.yeonjae.ovlo.application.port.out.chat;

import me.yeonjae.ovlo.domain.chat.model.ChatRoom;

public interface SaveChatPort {

    ChatRoom save(ChatRoom chatRoom);
}
