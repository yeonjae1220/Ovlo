package me.yeonjae.ovlo.application.port.out.chat;

import me.yeonjae.ovlo.domain.chat.model.ChatRoomId;
import me.yeonjae.ovlo.domain.member.model.MemberId;

public interface SaveReadMarkerPort {
    void markRead(ChatRoomId chatRoomId, MemberId memberId);
}
