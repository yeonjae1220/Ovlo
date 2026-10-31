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

    /** GROUP 방에 참여자를 더한다(이미 있으면 무시). DM 은 2명 고정이라 거부. */
    public boolean addParticipant(MemberId memberId) {
        Objects.requireNonNull(memberId, "memberId는 필수입니다");
        if (type == ChatRoomType.DM) {
            throw new IllegalStateException("DM 채팅방에는 참여자를 더할 수 없습니다");
        }
        if (participants.contains(memberId)) return false;
        participants.add(memberId);
        return true;
    }

    /** GROUP 방에서 참여자를 뺀다. 2명 미만이 되는 제거는 거부한다(방이 무의미해진다). 없으면 무시. */
    public boolean removeParticipant(MemberId memberId) {
        Objects.requireNonNull(memberId, "memberId는 필수입니다");
        if (type == ChatRoomType.DM) {
            throw new IllegalStateException("DM 채팅방에서는 참여자를 뺄 수 없습니다");
        }
        if (!participants.contains(memberId)) return false;
        if (participants.size() <= 2) {
            throw new IllegalStateException("채팅방 참여자는 최소 2명 이상이어야 합니다");
        }
        participants.remove(memberId);
        return true;
    }

    public boolean hasParticipant(MemberId memberId) {
        return participants.contains(memberId);
    }

    public ChatRoomId getId() { return id; }
    public ChatRoomType getType() { return type; }
    public String getName() { return name; }
    public List<MemberId> getParticipants() { return Collections.unmodifiableList(participants); }
    public List<Message> getMessages() { return Collections.unmodifiableList(messages); }
}
