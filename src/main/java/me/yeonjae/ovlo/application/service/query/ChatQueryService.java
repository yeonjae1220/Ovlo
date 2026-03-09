package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.ChatRoomResult;
import me.yeonjae.ovlo.application.dto.result.MessageResult;
import me.yeonjae.ovlo.application.port.in.chat.GetChatRoomQuery;
import me.yeonjae.ovlo.application.port.out.chat.LoadChatPort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.domain.chat.exception.ChatException;
import me.yeonjae.ovlo.domain.chat.model.ChatRoom;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomId;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ChatQueryService implements GetChatRoomQuery {

    private final LoadChatPort loadChatPort;
    private final LoadMemberPort loadMemberPort;

    public ChatQueryService(LoadChatPort loadChatPort, LoadMemberPort loadMemberPort) {
        this.loadChatPort = loadChatPort;
        this.loadMemberPort = loadMemberPort;
    }

    @Override
    public ChatRoomResult getChatRoom(Long chatRoomId) {
        ChatRoom room = loadChatPort.findById(new ChatRoomId(chatRoomId))
                .orElseThrow(() -> new ChatException("채팅방을 찾을 수 없습니다"));
        var info = buildParticipantInfo(room);
        var lastReadAt = loadChatPort.findAllLastReadAt(new ChatRoomId(chatRoomId));
        return ChatRoomResult.from(room, info.nicknames(), info.profileImages(), 0, lastReadAt);
    }

    @Override
    public List<ChatRoomResult> getChatRooms(Long memberId) {
        return loadChatPort.findByMemberId(new MemberId(memberId))
                .stream()
                .map(room -> {
                    var info = buildParticipantInfo(room);
                    ChatRoomId roomId = room.getId();
                    var lastReadAt = loadChatPort.findAllLastReadAt(roomId);
                    LocalDateTime myLastRead = loadChatPort
                            .findLastReadAt(roomId, new MemberId(memberId))
                            .orElse(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                    int unread = (int) loadChatPort.countUnread(roomId, new MemberId(memberId), myLastRead);
                    return ChatRoomResult.from(room, info.nicknames(), info.profileImages(), unread, lastReadAt);
                })
                .toList();
    }

    @Override
    public List<MessageResult> getMessages(Long chatRoomId, int page, int size) {
        return loadChatPort.findMessages(new ChatRoomId(chatRoomId), page, size)
                .stream()
                .map(MessageResult::from)
                .toList();
    }

    @Override
    public boolean isMemberOfRoom(Long chatRoomId, Long memberId) {
        return loadChatPort.findById(new ChatRoomId(chatRoomId))
                .map(room -> room.getParticipants().stream()
                        .anyMatch(pid -> pid.value().equals(memberId)))
                .orElse(false);
    }

    private record ParticipantInfo(Map<Long, String> nicknames, Map<Long, String> profileImages) {}

    private ParticipantInfo buildParticipantInfo(ChatRoom room) {
        List<Member> members = room.getParticipants().stream()
                .flatMap(pid -> loadMemberPort.findById(pid).stream())
                .toList();
        Map<Long, String> nicknames = members.stream().collect(
                Collectors.toMap(m -> m.getId().value(), Member::getNickname));
        Map<Long, String> profileImages = members.stream()
                .filter(m -> m.getProfileImageMediaId() != null)
                .collect(Collectors.toMap(m -> m.getId().value(), Member::getProfileImageMediaId));
        return new ParticipantInfo(nicknames, profileImages);
    }
}
