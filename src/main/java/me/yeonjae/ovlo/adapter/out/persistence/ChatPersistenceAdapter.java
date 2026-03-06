package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.mapper.ChatMapper;
import me.yeonjae.ovlo.adapter.out.persistence.repository.ChatRoomJpaRepository;
import me.yeonjae.ovlo.adapter.out.persistence.repository.MessageJpaRepository;
import me.yeonjae.ovlo.application.port.out.chat.LoadChatPort;
import me.yeonjae.ovlo.application.port.out.chat.SaveChatPort;
import me.yeonjae.ovlo.domain.chat.model.ChatRoom;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomId;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomType;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class ChatPersistenceAdapter implements LoadChatPort, SaveChatPort {

    private final ChatRoomJpaRepository chatRoomJpaRepository;
    private final MessageJpaRepository messageJpaRepository;
    private final ChatMapper chatMapper;

    public ChatPersistenceAdapter(ChatRoomJpaRepository chatRoomJpaRepository,
                                  MessageJpaRepository messageJpaRepository,
                                  ChatMapper chatMapper) {
        this.chatRoomJpaRepository = chatRoomJpaRepository;
        this.messageJpaRepository = messageJpaRepository;
        this.chatMapper = chatMapper;
    }

    @Override
    public Optional<ChatRoom> findById(ChatRoomId chatRoomId) {
        return chatRoomJpaRepository.findById(chatRoomId.value()).map(entity -> {
            var messages = messageJpaRepository.findByChatRoomIdOrderBySentAtAsc(entity.getId());
            return chatMapper.toDomain(entity, messages);
        });
    }

    @Override
    public List<ChatRoom> findByMemberId(MemberId memberId) {
        // 목록 조회: 메시지 로드 없음 (ChatRoomResult에 메시지 미포함)
        return chatRoomJpaRepository.findByMemberId(memberId.value())
                .stream()
                .map(entity -> chatMapper.toDomain(entity, java.util.Collections.emptyList()))
                .toList();
    }

    @Override
    public boolean existsDmRoom(MemberId memberId1, MemberId memberId2) {
        return chatRoomJpaRepository.findDmRoomId(ChatRoomType.DM, memberId1.value(), memberId2.value()).isPresent();
    }

    @Override
    public ChatRoom save(ChatRoom chatRoom) {
        var entity = chatMapper.toJpaEntity(chatRoom);
        var saved = chatRoomJpaRepository.save(entity);

        // Save new messages (id == null)
        chatRoom.getMessages().stream()
                .filter(m -> m.getId() == null)
                .forEach(m -> messageJpaRepository.save(chatMapper.toMessageJpaEntity(saved.getId(), m)));

        var messages = messageJpaRepository.findByChatRoomIdOrderBySentAtAsc(saved.getId());
        return chatMapper.toDomain(saved, messages);
    }
}
