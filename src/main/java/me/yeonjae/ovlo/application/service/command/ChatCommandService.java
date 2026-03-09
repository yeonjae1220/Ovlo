package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.CreateChatRoomCommand;
import me.yeonjae.ovlo.application.dto.command.SendMessageCommand;
import me.yeonjae.ovlo.application.dto.result.ChatRoomResult;
import me.yeonjae.ovlo.application.dto.result.MessageResult;
import me.yeonjae.ovlo.application.port.in.chat.CreateChatRoomUseCase;
import me.yeonjae.ovlo.application.port.in.chat.MarkMessagesReadUseCase;
import me.yeonjae.ovlo.application.port.in.chat.SendMessageUseCase;
import me.yeonjae.ovlo.application.port.out.chat.LoadChatPort;
import me.yeonjae.ovlo.application.port.out.chat.SaveChatPort;
import me.yeonjae.ovlo.application.port.out.chat.SaveReadMarkerPort;
import me.yeonjae.ovlo.domain.chat.exception.ChatException;
import me.yeonjae.ovlo.domain.chat.model.ChatRoom;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomId;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomType;
import me.yeonjae.ovlo.domain.chat.model.Message;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ChatCommandService implements CreateChatRoomUseCase, SendMessageUseCase, MarkMessagesReadUseCase {

    private final LoadChatPort loadChatPort;
    private final SaveChatPort saveChatPort;
    private final SaveReadMarkerPort saveReadMarkerPort;

    public ChatCommandService(LoadChatPort loadChatPort, SaveChatPort saveChatPort,
                              SaveReadMarkerPort saveReadMarkerPort) {
        this.loadChatPort = loadChatPort;
        this.saveChatPort = saveChatPort;
        this.saveReadMarkerPort = saveReadMarkerPort;
    }

    @Override
    public ChatRoomResult createChatRoom(CreateChatRoomCommand command) {
        List<MemberId> participants = command.participantIds().stream()
                .map(MemberId::new)
                .toList();

        if (command.type() == ChatRoomType.DM) {
            if (participants.size() < 2) {
                throw new ChatException("DM 채팅방은 참여자 2명이 필요합니다");
            }
            MemberId m1 = participants.get(0);
            MemberId m2 = participants.get(1);
            Optional<ChatRoomId> existingRoomId = loadChatPort.findDmRoomId(m1, m2);
            if (existingRoomId.isPresent()) {
                ChatRoom existingRoom = loadChatPort.findById(existingRoomId.get())
                        .orElseThrow(() -> new ChatException("채팅방을 찾을 수 없습니다"));
                return ChatRoomResult.from(existingRoom);
            }
        }

        ChatRoom room = ChatRoom.create(command.type(), command.name(), participants);
        try {
            ChatRoom saved = saveChatPort.save(room);
            return ChatRoomResult.from(saved);
        } catch (DataIntegrityViolationException e) {
            // TODO: V7 migration에 DM unique constraint 추가 시 이 catch가 TOCTOU race condition 최종 방어선 역할
            throw new ChatException("이미 해당 두 회원 간의 DM 채팅방이 존재합니다");
        }
    }

    @Override
    public void markRead(Long chatRoomId, Long memberId) {
        saveReadMarkerPort.markRead(new ChatRoomId(chatRoomId), new MemberId(memberId));
    }

    @Override
    public MessageResult sendMessage(SendMessageCommand command) {
        ChatRoom room = loadChatPort.findById(new ChatRoomId(command.chatRoomId()))
                .orElseThrow(() -> new ChatException("채팅방을 찾을 수 없습니다"));

        MemberId senderId = new MemberId(command.senderId());
        room.addMessage(senderId, command.content());
        ChatRoom saved = saveChatPort.save(room);
        List<Message> savedMessages = saved.getMessages();
        return MessageResult.from(savedMessages.get(savedMessages.size() - 1));
    }
}
