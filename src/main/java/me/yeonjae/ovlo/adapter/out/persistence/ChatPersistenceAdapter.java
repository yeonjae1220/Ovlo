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

    private final OfficePersonalDataAdapter privacy;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final ChatRoomJpaRepository chatRoomJpaRepository;
    private final MessageJpaRepository messageJpaRepository;
    private final ChatRoomReadMarkerRepository readMarkerRepository;
    private final ChatMapper chatMapper;

    public ChatPersistenceAdapter(OfficePersonalDataAdapter privacy, ChatRoomJpaRepository chatRoomJpaRepository,
                                  MessageJpaRepository messageJpaRepository,
                                  ChatRoomReadMarkerRepository readMarkerRepository,
                                  ChatMapper chatMapper, org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.privacy=privacy;
        this.chatRoomJpaRepository = chatRoomJpaRepository;
        this.messageJpaRepository = messageJpaRepository;
        this.readMarkerRepository = readMarkerRepository;
        this.chatMapper = chatMapper;
    }

    @Override
    public Optional<ChatRoom> findById(ChatRoomId chatRoomId) {
        // Messages have a separate bounded page API.
        return chatRoomJpaRepository.findById(chatRoomId.value())
                .map(entity -> chatMapper.toDomain(entity, List.of()));
    }

    @Override
    public List<ChatRoom> findByMemberId(MemberId memberId) {
        record RoomRow(long id, ChatRoomType type, String name) {}
        var rooms = jdbc.query("SELECT cr.id,cr.type,cr.name FROM chat_room cr WHERE "
                        + ChatRoomJpaRepository.ACCESS.replace(":memberId","?") + " ORDER BY cr.created_at DESC,cr.id DESC",
                (row,index) -> new RoomRow(row.getLong(1),ChatRoomType.valueOf(row.getString(2)),row.getString(3)),
                memberId.value(),memberId.value(),memberId.value());
        if (rooms.isEmpty()) return List.of();
        var participants = new java.util.HashMap<Long,java.util.List<MemberId>>();
        new org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(jdbc).query(
                "SELECT chat_room_id,member_id FROM chat_room_participant WHERE chat_room_id IN (:ids) ORDER BY chat_room_id,member_id",
                Map.of("ids",rooms.stream().map(RoomRow::id).toList()),
                (org.springframework.jdbc.core.RowCallbackHandler) row -> participants.computeIfAbsent(row.getLong(1),id -> new java.util.ArrayList<>()).add(new MemberId(row.getLong(2))));
        return rooms.stream().map(row -> ChatRoom.restore(new ChatRoomId(row.id()),row.type(),row.name(),
                participants.getOrDefault(row.id(),List.of()),List.of())).toList();
    }

    @Override
    public Optional<ChatRoomId> findDmRoomId(MemberId memberId1, MemberId memberId2) {
        return chatRoomJpaRepository.findDmRoomId(ChatRoomType.DM, memberId1.value(), memberId2.value())
                .map(ChatRoomId::new);
    }

    @Override
    @Transactional
    public ChatRoom save(ChatRoom chatRoom) {
        if (chatRoom.getId() == null && chatRoom.getType() == ChatRoomType.DM) {
            var ids = chatRoom.getParticipants().stream().map(MemberId::value).sorted().toList();
            // Serializes every SaveChatPort creation path, including marketplace DMs.
            var active = jdbc.queryForList("SELECT status FROM member WHERE id IN (?,?) ORDER BY id FOR UPDATE",
                    String.class, ids.get(0), ids.get(1));
            if (active.size() != 2 || active.stream().anyMatch(s -> !"ACTIVE".equals(s))) {
                throw new me.yeonjae.ovlo.domain.chat.exception.ChatException("사용할 수 없는 참여자입니다",
                        me.yeonjae.ovlo.domain.chat.exception.ChatException.ErrorType.NOT_FOUND);
            }
            var existing = findDmRoomId(new MemberId(ids.get(0)), new MemberId(ids.get(1)));
            if (existing.isPresent()) return findById(existing.get()).orElseThrow();
        }
        var entity = chatMapper.toJpaEntity(chatRoom);
        var saved = chatRoomJpaRepository.save(entity);
        saveNewMessages(saved.getId(), chatRoom.getMessages());
        return chatMapper.toDomain(saved, List.of());
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
        var result = new java.util.HashMap<Long, Long>();
        sinceByRoomId.keySet().forEach(id -> result.put(id, 0L));
        // One grouped query with the caller's exact per-room cutoffs; no N+1.
        String values = sinceByRoomId.keySet().stream().map(id -> "(?::bigint,?::timestamptz)").collect(Collectors.joining(","));
        var args = new java.util.ArrayList<Object>();
        sinceByRoomId.forEach((id, since) -> { args.add(id); args.add(java.sql.Timestamp.from(since)); });
        args.add(memberId.value());
        jdbc.query("SELECT m.chat_room_id, COUNT(*) FROM message m JOIN (VALUES " + values
                + ") AS cut(room_id,since) ON cut.room_id=m.chat_room_id WHERE m.sender_id<>?"
                + " AND m.sent_at>cut.since AND m.hidden_by_withdrawal=false"
                + " AND NOT EXISTS (SELECT 1 FROM office_consultation c WHERE c.chat_room_id=m.chat_room_id"
                + " AND (c.personal_data_erased_at IS NOT NULL OR c.personal_data_expires_at<=CURRENT_TIMESTAMP))"
                + " GROUP BY m.chat_room_id",
                (org.springframework.jdbc.core.RowCallbackHandler) row -> result.put(row.getLong(1), row.getLong(2)), args.toArray());
        return result;
    }

    @Override
    @Transactional
    public void markRead(ChatRoomId chatRoomId, MemberId memberId) {
        // The parent exists before the first marker. Lock it to serialize concurrent initial
        // reads on both PostgreSQL and the local H2 profile; locking an absent marker cannot do that.
        jdbc.queryForObject("SELECT id FROM chat_room WHERE id=? FOR UPDATE",Long.class,chatRoomId.value());
        var now=java.sql.Timestamp.from(Instant.now());
        int updated=jdbc.update("UPDATE chat_room_read_marker SET last_read_at=GREATEST(last_read_at,?) WHERE chat_room_id=? AND member_id=?",
                now,chatRoomId.value(),memberId.value());
        if(updated==0) jdbc.update("INSERT INTO chat_room_read_marker(chat_room_id,member_id,last_read_at) VALUES(?,?,?)",
                chatRoomId.value(),memberId.value(),now);
    }

    @Override
    @Transactional
    public Message saveMessage(Long chatRoomId, Long senderId, String content) {
        privacy.lockRoom(chatRoomId);
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
        if(messages.stream().anyMatch(m -> m.getId()==null)) privacy.lockRoom(chatRoomId);
        messages.stream()
                .filter(m -> m.getId() == null)
                .forEach(m -> messageJpaRepository.save(chatMapper.toMessageJpaEntity(chatRoomId, m)));
    }
}
