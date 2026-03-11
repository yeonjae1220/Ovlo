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
import java.util.Optional;
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
                .orElseThrow(() -> new ChatException("채팅방을 찾을 수 없습니다", ChatException.ErrorType.NOT_FOUND));
        var info = buildParticipantInfo(room);
        var lastReadAt = loadChatPort.findAllLastReadAt(new ChatRoomId(chatRoomId));
        return ChatRoomResult.from(room, info.nicknames(), info.profileImages(), 0, lastReadAt);
    }

    @Override
    public List<ChatRoomResult> getChatRooms(Long memberId) {
        List<ChatRoom> rooms = loadChatPort.findByMemberId(new MemberId(memberId));
        if (rooms.isEmpty()) return List.of();

        // 배치 1: 전체 채팅방 참여자 ID 수집 → 회원 정보 한 번에 조회 (N×P → 1 쿼리)
        List<MemberId> allParticipantIds = rooms.stream()
                .flatMap(r -> r.getParticipants().stream())
                .distinct()
                .toList();
        Map<Long, Member> memberById = loadMemberPort.findAllByIds(allParticipantIds).stream()
                .collect(Collectors.toMap(m -> m.getId().value(), m -> m));

        // 배치 2: 전체 채팅방 읽음 마커 한 번에 조회 (2N → 1 쿼리)
        List<ChatRoomId> roomIds = rooms.stream().map(ChatRoom::getId).toList();
        Map<Long, Map<Long, LocalDateTime>> lastReadAtByRoom =
                loadChatPort.findAllLastReadAtByRoomIds(roomIds);

        // 배치 3: 전체 채팅방 읽음 기준 시각 수집 → 미읽음 수 일괄 조회 (N → 1 배치)
        Map<Long, LocalDateTime> sinceByRoomId = rooms.stream()
                .collect(Collectors.toMap(
                        r -> r.getId().value(),
                        r -> Optional.ofNullable(lastReadAtByRoom.getOrDefault(r.getId().value(), Map.of()).get(memberId))
                                .orElse(LocalDateTime.of(1970, 1, 1, 0, 0, 0))
                ));
        Map<Long, Long> unreadByRoom = loadChatPort.countUnreadBatch(new MemberId(memberId), sinceByRoomId);

        return rooms.stream()
                .map(room -> {
                    ParticipantInfo info = buildParticipantInfoFromCache(room, memberById);
                    Map<Long, LocalDateTime> roomReadAt =
                            lastReadAtByRoom.getOrDefault(room.getId().value(), Map.of());
                    int unread = unreadByRoom.getOrDefault(room.getId().value(), 0L).intValue();
                    return ChatRoomResult.from(room, info.nicknames(), info.profileImages(), unread, roomReadAt);
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
        return loadChatPort.isMember(new ChatRoomId(chatRoomId), new MemberId(memberId));
    }

    private record ParticipantInfo(Map<Long, String> nicknames, Map<Long, String> profileImages) {}

    private ParticipantInfo buildParticipantInfo(ChatRoom room) {
        List<Member> members = room.getParticipants().stream()
                .flatMap(pid -> loadMemberPort.findById(pid).stream())
                .toList();
        return toParticipantInfo(members);
    }

    private ParticipantInfo buildParticipantInfoFromCache(ChatRoom room, Map<Long, Member> cache) {
        List<Member> members = room.getParticipants().stream()
                .map(pid -> cache.get(pid.value()))
                .filter(m -> m != null)
                .toList();
        return toParticipantInfo(members);
    }

    private ParticipantInfo toParticipantInfo(List<Member> members) {
        Map<Long, String> nicknames = members.stream().collect(
                Collectors.toMap(m -> m.getId().value(), Member::getNickname));
        Map<Long, String> profileImages = members.stream()
                .filter(m -> m.getProfileImageMediaId() != null)
                .collect(Collectors.toMap(m -> m.getId().value(), Member::getProfileImageMediaId));
        return new ParticipantInfo(nicknames, profileImages);
    }
}
