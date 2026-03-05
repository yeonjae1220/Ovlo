package me.yeonjae.ovlo.domain.chat.model;

import me.yeonjae.ovlo.domain.member.model.MemberId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ChatRoom {

    private ChatRoomId id;
    private ChatRoomType type;
    private String name;
    private List<MemberId> participants;
    private List<Message> messages;

    private ChatRoom() {}

    public static ChatRoom create(ChatRoomType type, String name, List<MemberId> participants) {
        Objects.requireNonNull(type, "채팅방 타입은 필수입니다");
        Objects.requireNonNull(participants, "참여자 목록은 필수입니다");
        if (participants.size() < 2) {
            throw new IllegalArgumentException("채팅방 참여자는 최소 2명 이상이어야 합니다");
        }
        if (type == ChatRoomType.DM && participants.size() != 2) {
            throw new IllegalArgumentException("DM 채팅방은 정확히 2명이어야 합니다");
        }

        ChatRoom room = new ChatRoom();
        room.type = type;
        room.name = name;
        room.participants = new ArrayList<>(participants);
        room.messages = new ArrayList<>();
        return room;
    }

    /** persistence 계층 전용 */
    public static ChatRoom restore(ChatRoomId id, ChatRoomType type, String name,
                                   List<MemberId> participants, List<Message> messages) {
        ChatRoom room = new ChatRoom();
        room.id = id;
        room.type = type;
        room.name = name;
        room.participants = new ArrayList<>(participants);
        room.messages = new ArrayList<>(messages);
        return room;
    }

    public Message addMessage(MemberId senderId, String content) {
        if (!participants.contains(senderId)) {
            throw new IllegalArgumentException("채팅방 참여자만 메시지를 보낼 수 있습니다");
        }
        Message message = Message.create(senderId, content);
        messages.add(message);
        return message;
    }

    public ChatRoomId getId() { return id; }
    public ChatRoomType getType() { return type; }
    public String getName() { return name; }
    public List<MemberId> getParticipants() { return Collections.unmodifiableList(participants); }
    public List<Message> getMessages() { return Collections.unmodifiableList(messages); }
}
