package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.port.out.chat.LoadChatPort;
import me.yeonjae.ovlo.application.port.out.chat.SaveChatPort;
import me.yeonjae.ovlo.domain.chat.model.ChatRoom;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomId;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Chat JPA 구현 예정 (stub).
 * LoadChatPort / SaveChatPort 구현.
 */
@Component
public class ChatPersistenceAdapter implements LoadChatPort, SaveChatPort {

    @Override
    public Optional<ChatRoom> findById(ChatRoomId chatRoomId) {
        throw new UnsupportedOperationException("Chat JPA 구현 예정");
    }

    @Override
    public boolean existsDmRoom(MemberId memberId1, MemberId memberId2) {
        throw new UnsupportedOperationException("Chat JPA 구현 예정");
    }

    @Override
    public ChatRoom save(ChatRoom chatRoom) {
        throw new UnsupportedOperationException("Chat JPA 구현 예정");
    }
}
