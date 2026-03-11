package me.yeonjae.ovlo.application.port.out.chat;

import me.yeonjae.ovlo.domain.chat.model.ChatRoom;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomId;
import me.yeonjae.ovlo.domain.chat.model.Message;
import me.yeonjae.ovlo.domain.member.model.MemberId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoadChatPort {

    Optional<ChatRoom> findById(ChatRoomId chatRoomId);

    Optional<ChatRoomId> findDmRoomId(MemberId memberId1, MemberId memberId2);

    List<ChatRoom> findByMemberId(MemberId memberId);

    List<Message> findMessages(ChatRoomId chatRoomId, int page, int size);

    /** 채팅방 전체 참여자의 마지막 읽음 시각 (memberId → lastReadAt) */
    Map<Long, LocalDateTime> findAllLastReadAt(ChatRoomId chatRoomId);

    /** 여러 채팅방의 읽음 마커 일괄 조회 (chatRoomId → (memberId → lastReadAt)) */
    Map<Long, Map<Long, LocalDateTime>> findAllLastReadAtByRoomIds(List<ChatRoomId> roomIds);

    /** 특정 회원의 마지막 읽음 시각 */
    Optional<LocalDateTime> findLastReadAt(ChatRoomId chatRoomId, MemberId memberId);

    /** since 이후에 senderId가 아닌 사람이 보낸 메시지 수 */
    long countUnread(ChatRoomId chatRoomId, MemberId memberId, LocalDateTime since);

    /** 여러 채팅방의 안 읽은 메시지 수 일괄 조회 (chatRoomId → unreadCount) */
    Map<Long, Long> countUnreadBatch(MemberId memberId, Map<Long, LocalDateTime> sinceByRoomId);

    /** 채팅방 참여자 여부 (단일 EXISTS 쿼리) */
    boolean isMember(ChatRoomId chatRoomId, MemberId memberId);
}
