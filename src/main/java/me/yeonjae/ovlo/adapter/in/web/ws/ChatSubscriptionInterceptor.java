package me.yeonjae.ovlo.adapter.in.web.ws;

import me.yeonjae.ovlo.application.port.in.chat.GetChatRoomQuery;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

/** Only exact application destinations are accepted. Broker destinations are server-write-only. */
@Component
public class ChatSubscriptionInterceptor implements ChannelInterceptor {
    private static final Pattern TOPIC = Pattern.compile("^/topic/chat/([1-9][0-9]*)(/read)?$");
    private static final Pattern SEND = Pattern.compile("^/app/chat/([1-9][0-9]*)$");
    private final GetChatRoomQuery rooms;
    private final ChatOutboundAuthorizationInterceptor outbound;

    public ChatSubscriptionInterceptor(GetChatRoomQuery rooms, ChatOutboundAuthorizationInterceptor outbound) {
        this.rooms = rooms;
        this.outbound = outbound;
    }

    static Long topicRoom(String destination) { return roomId(TOPIC, destination); }

    private static Long roomId(Pattern pattern, String destination) {
        if (destination == null) return null;
        var match = pattern.matcher(destination);
        if (!match.matches()) return null;
        try { return Long.valueOf(match.group(1)); }
        catch (NumberFormatException e) { return null; }
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var headers = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (headers == null) return message;
        var command = headers.getCommand();
        if (command != StompCommand.SUBSCRIBE && command != StompCommand.SEND) return message;
        Long room = roomId(command == StompCommand.SUBSCRIBE ? TOPIC : SEND, headers.getDestination());
        var attributes = headers.getSessionAttributes();
        Object rawMember = attributes == null ? null : attributes.get(WsSessionKeys.MEMBER_ID);
        if (room == null || !(rawMember instanceof Long member) || !rooms.isMemberOfRoom(room, member)) {
            throw new MessageDeliveryException(message, "채팅방 접근 권한이 없습니다");
        }
        if (command == StompCommand.SUBSCRIBE) {
            if (headers.getSessionId() == null) throw new MessageDeliveryException(message, "세션이 필요합니다");
            outbound.register(headers.getSessionId(), (Long) rawMember);
        }
        return message;
    }
}
