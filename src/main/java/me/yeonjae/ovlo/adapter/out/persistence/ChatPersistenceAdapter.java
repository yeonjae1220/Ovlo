package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.entity.ChatRoomReadMarkerJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.mapper.ChatMapper;
import me.yeonjae.ovlo.adapter.out.persistence.repository.ChatRoomJpaRepository;
import me.yeonjae.ovlo.adapter.out.persistence.repository.ChatRoomReadMarkerRepository;
import me.yeonjae.ovlo.adapter.out.persistence.repository.MessageJpaRepository;
import me.yeonjae.ovlo.application.port.out.chat.LoadChatPort;
import me.yeonjae.ovlo.application.port.out.chat.SaveChatPort;
import me.yeonjae.ovlo.application.port.out.chat.SaveReadMarkerPort;
import me.yeonjae.ovlo.domain.chat.model.ChatRoom;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomId;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomType;
import me.yeonjae.ovlo.domain.chat.model.Message;
import me.yeonjae.ovlo.domain.chat.model.MessageId;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ChatPersistenceAdapter implements LoadChatPort, SaveChatPort, SaveReadMarkerPort {

    private final ChatRoomJpaRepository chatRoomJpaRepository;
    private final MessageJpaRepository messageJpaRepository;
    private final ChatRoomReadMarkerRepository readMarkerRepository;
    private final ChatMapper chatMapper;

    public ChatPersistenceAdapter(ChatRoomJpaRepository chatRoomJpaRepository,
                                  MessageJpaRepository messageJpaRepository,
                                  ChatRoomReadMarkerRepository readMarkerRepository,
                                  ChatMapper chatMapper) {
        this.chatRoomJpaRepository = chatRoomJpaRepository;
        this.messageJpaRepository = messageJpaRepository;
        this.readMarkerRepository = readMarkerRepository;
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
        return chatRoomJpaRepository.findByMemberId(memberId.value())
                .stream()
                .map(entity -> chatMapper.toDomain(entity, Collections.emptyList()))
                .toList();
    }

    @Override
    public boolean existsDmRoom(MemberId memberId1, MemberId memberId2) {
        return chatRoomJpaRepository.findDmRoomId(ChatRoomType.DM, memberId1.value(), memberId2.value()).isPresent();
    }

    @Override
    @Transactional
    public ChatRoom save(ChatRoom chatRoom) {
        var entity = chatMapper.toJpaEntity(chatRoom);
        var saved = chatRoomJpaRepository.save(entity);
        saveNewMessages(saved.getId(), chatRoom.getMessages());
        var messages = messageJpaRepository.findByChatRoomIdOrderBySentAtAsc(saved.getId());
        return chatMapper.toDomain(saved, messages);
    }

    @Override
    public List<Message> findMessages(ChatRoomId chatRoomId, int page, int size) {
        var entities = messageJpaRepository.findByChatRoomIdOrderBySentAtDesc(
                chatRoomId.value(), PageRequest.of(page, size));
        return entities.reversed().stream()
                .map(m -> Message.restore(
                        new MessageId(m.getId()),
                        new MemberId(m.getSenderId()),
                        m.getContent(),
                        m.getSentAt()))
                .toList();
    }

    @Override
    public Map<Long, LocalDateTime> findAllLastReadAt(ChatRoomId chatRoomId) {
        return readMarkerRepository.findAllByChatRoomId(chatRoomId.value()).stream()
                .collect(Collectors.toMap(
                        m -> m.getId().getMemberId(),
                        ChatRoomReadMarkerJpaEntity::getLastReadAt
                ));
    }

    @Override
    public Optional<LocalDateTime> findLastReadAt(ChatRoomId chatRoomId, MemberId memberId) {
        return readMarkerRepository.findById(
                new ChatRoomReadMarkerJpaEntity.ReadMarkerId(chatRoomId.value(), memberId.value())
        ).map(ChatRoomReadMarkerJpaEntity::getLastReadAt);
    }

    @Override
    public long countUnread(ChatRoomId chatRoomId, MemberId memberId, LocalDateTime since) {
        return messageJpaRepository.countUnread(chatRoomId.value(), memberId.value(), since);
    }

    @Override
    @Transactional
    public void markRead(ChatRoomId chatRoomId, MemberId memberId) {
        var id = new ChatRoomReadMarkerJpaEntity.ReadMarkerId(chatRoomId.value(), memberId.value());
        var marker = readMarkerRepository.findById(id)
                .orElse(new ChatRoomReadMarkerJpaEntity(chatRoomId.value(), memberId.value()));
        marker.setLastReadAt(LocalDateTime.now());
        readMarkerRepository.save(marker);
    }

    private void saveNewMessages(Long chatRoomId, List<Message> messages) {
        messages.stream()
                .filter(m -> m.getId() == null)
                .forEach(m -> messageJpaRepository.save(chatMapper.toMessageJpaEntity(chatRoomId, m)));
    }
}
