package me.yeonjae.ovlo.application.port.out.chat;

import me.yeonjae.ovlo.domain.chat.model.ChatRoom;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomId;
import me.yeonjae.ovlo.domain.member.model.MemberId;

import java.util.Optional;

public interface LoadChatPort {

    Optional<ChatRoom> findById(ChatRoomId chatRoomId);

    boolean existsDmRoom(MemberId memberId1, MemberId memberId2);
}
