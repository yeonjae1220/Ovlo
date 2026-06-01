package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.entity.ChatRoomReadMarkerJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.entity.MessageJpaEntity;
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

import java.time.Instant;
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
        // 단건 조회: 메시지 전체 로딩 (채팅방 상세 화면용, 페이지네이션은 findMessages() 사용)
        return chatRoomJpaRepository.findById(chatRoomId.value()).map(entity -> {
            var messages = messageJpaRepository.findByChatRoomIdAndHiddenByWithdrawalFalseOrderBySentAtAsc(entity.getId());
            return chatMapper.toDomain(entity, messages);
        });
    }

    @Override
    public List<ChatRoom> findByMemberId(MemberId memberId) {
        // 목록 조회: 메시지 로딩 생략 (N×M 로딩 방지 — 메시지는 입장 시 별도 조회)
        return chatRoomJpaRepository.findByMemberId(memberId.value())
                .stream()
                .map(entity -> chatMapper.toDomain(entity, Collections.emptyList()))
                .toList();
    }

    @Override
    public Optional<ChatRoomId> findDmRoomId(MemberId memberId1, MemberId memberId2) {
        return chatRoomJpaRepository.findDmRoomId(ChatRoomType.DM, memberId1.value(), memberId2.value())
                .map(ChatRoomId::new);
    }

    @Override
    @Transactional
    public ChatRoom save(ChatRoom chatRoom) {
        var entity = chatMapper.toJpaEntity(chatRoom);
        var saved = chatRoomJpaRepository.save(entity);
        saveNewMessages(saved.getId(), chatRoom.getMessages());
        var messages = messageJpaRepository.findByChatRoomIdAndHiddenByWithdrawalFalseOrderBySentAtAsc(saved.getId());
        return chatMapper.toDomain(saved, messages);
    }

    @Override
    public List<Message> findMessages(ChatRoomId chatRoomId, int page, int size) {
        var entities = messageJpaRepository.findByChatRoomIdAndHiddenByWithdrawalFalseOrderBySentAtDesc(
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
    public Map<Long, Instant> findAllLastReadAt(ChatRoomId chatRoomId) {
        return readMarkerRepository.findAllByChatRoomId(chatRoomId.value()).stream()
                .collect(Collectors.toMap(
                        m -> m.getId().getMemberId(),
                        ChatRoomReadMarkerJpaEntity::getLastReadAt
                ));
    }

    @Override
    public Map<Long, Map<Long, Instant>> findAllLastReadAtByRoomIds(List<ChatRoomId> roomIds) {
        if (roomIds.isEmpty()) return Map.of();
        List<Long> rawIds = roomIds.stream().map(ChatRoomId::value).toList();
        return readMarkerRepository.findAllByChatRoomIdIn(rawIds).stream()
                .collect(Collectors.groupingBy(
                        m -> m.getId().getChatRoomId(),
                        Collectors.toMap(
                                m -> m.getId().getMemberId(),
                                ChatRoomReadMarkerJpaEntity::getLastReadAt
                        )
                ));
    }

    @Override
    public Optional<Instant> findLastReadAt(ChatRoomId chatRoomId, MemberId memberId) {
        return readMarkerRepository.findById(
                new ChatRoomReadMarkerJpaEntity.ReadMarkerId(chatRoomId.value(), memberId.value())
        ).map(ChatRoomReadMarkerJpaEntity::getLastReadAt);
    }

    @Override
    public boolean isMember(ChatRoomId chatRoomId, MemberId memberId) {
        return chatRoomJpaRepository.existsMember(chatRoomId.value(), memberId.value());
    }

    @Override
    public long countUnread(ChatRoomId chatRoomId, MemberId memberId, Instant since) {
        return messageJpaRepository.countUnread(chatRoomId.value(), memberId.value(), since);
    }

    @Override
    public Map<Long, Long> countUnreadBatch(MemberId memberId, Map<Long, Instant> sinceByRoomId) {
        if (sinceByRoomId.isEmpty()) return Map.of();
        return sinceByRoomId.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> messageJpaRepository.countUnread(e.getKey(), memberId.value(), e.getValue())
                ));
    }

    @Override
    @Transactional
    public void markRead(ChatRoomId chatRoomId, MemberId memberId) {
        var id = new ChatRoomReadMarkerJpaEntity.ReadMarkerId(chatRoomId.value(), memberId.value());
        var marker = readMarkerRepository.findById(id)
                .orElse(new ChatRoomReadMarkerJpaEntity(chatRoomId.value(), memberId.value()));
        marker.setLastReadAt(Instant.now());
        readMarkerRepository.save(marker);
    }

    @Override
    @Transactional
    public Message saveMessage(Long chatRoomId, Long senderId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("메시지 내용은 빈 값일 수 없습니다");
        }
        var entity = new MessageJpaEntity();
        entity.setChatRoomId(chatRoomId);
        entity.setSenderId(senderId);
        entity.setContent(content);
        entity.setSentAt(Instant.now());
        var saved = messageJpaRepository.save(entity);
        return Message.restore(
                new MessageId(saved.getId()),
                new MemberId(saved.getSenderId()),
                saved.getContent(),
                saved.getSentAt());
    }

    private void saveNewMessages(Long chatRoomId, List<Message> messages) {
        messages.stream()
                .filter(m -> m.getId() == null)
                .forEach(m -> messageJpaRepository.save(chatMapper.toMessageJpaEntity(chatRoomId, m)));
    }
}
