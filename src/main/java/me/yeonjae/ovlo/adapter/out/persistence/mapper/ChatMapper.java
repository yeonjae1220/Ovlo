package me.yeonjae.ovlo.adapter.out.persistence.mapper;

import me.yeonjae.ovlo.adapter.out.persistence.entity.ChatRoomJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.entity.MessageJpaEntity;
import me.yeonjae.ovlo.domain.chat.model.*;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ChatMapper {

    public ChatRoomJpaEntity toJpaEntity(ChatRoom room) {
        ChatRoomJpaEntity entity = new ChatRoomJpaEntity();
        if (room.getId() != null) {
            entity.setId(room.getId().value());
        }
        entity.setType(room.getType());
        entity.setName(room.getName());
        entity.setParticipantIds(room.getParticipants().stream().map(MemberId::value).toList());
        return entity;
    }

    public MessageJpaEntity toMessageJpaEntity(Long chatRoomId, Message message) {
        MessageJpaEntity entity = new MessageJpaEntity();
        entity.setChatRoomId(chatRoomId);
        entity.setSenderId(message.getSenderId().value());
        entity.setContent(message.getContent());
        entity.setSentAt(message.getSentAt());
        return entity;
    }

    public ChatRoom toDomain(ChatRoomJpaEntity entity, List<MessageJpaEntity> messages) {
        List<MemberId> participants = entity.getParticipantIds().stream()
                .map(MemberId::new).toList();
        List<Message> domainMessages = messages.stream()
                .map(m -> Message.restore(
                        new MessageId(m.getId()),
                        new MemberId(m.getSenderId()),
                        m.getContent(),
                        m.getSentAt()))
                .toList();
        return ChatRoom.restore(
                new ChatRoomId(entity.getId()),
                entity.getType(),
                entity.getName(),
                participants,
                domainMessages
        );
    }
}
