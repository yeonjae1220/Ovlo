package me.yeonjae.ovlo.adapter.in.web.ws;

import me.yeonjae.ovlo.application.port.in.chat.GetChatRoomQuery;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STOMP SUBSCRIBE 프레임에서 채팅방 참여자 여부를 검증한다.
 * /topic/chat/{roomId} 또는 /topic/chat/{roomId}/read 구독 시 적용.
 */
@Component
public class ChatSubscriptionInterceptor implements ChannelInterceptor {

    private static final Pattern CHAT_TOPIC = Pattern.compile("^/topic/chat/(\\d+)(/read)?$");

    private final GetChatRoomQuery getChatRoomQuery;

    public ChatSubscriptionInterceptor(GetChatRoomQuery getChatRoomQuery) {
        this.getChatRoomQuery = getChatRoomQuery;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        String destination = accessor.getDestination();
        if (destination == null) return message;

        Matcher matcher = CHAT_TOPIC.matcher(destination);
        if (!matcher.matches()) return message;

        Long memberId = (Long) accessor.getSessionAttributes().get("memberId");
        if (memberId == null) {
            throw new IllegalArgumentException("인증이 필요합니다");
        }

        Long roomId = Long.parseLong(matcher.group(1));
        if (!getChatRoomQuery.isMemberOfRoom(roomId, memberId)) {
            throw new IllegalArgumentException("채팅방 접근 권한이 없습니다");
        }

        return message;
    }
}
