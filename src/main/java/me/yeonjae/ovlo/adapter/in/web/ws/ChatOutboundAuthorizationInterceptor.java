package me.yeonjae.ovlo.adapter.in.web.ws;

import me.yeonjae.ovlo.application.port.in.chat.GetChatRoomQuery;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.*;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import java.util.concurrent.ConcurrentHashMap;

/** Recheck every delivery: an existing subscription must not outlive membership or account access. */
@Component
public class ChatOutboundAuthorizationInterceptor implements ChannelInterceptor {
    private final GetChatRoomQuery rooms;
    private final ConcurrentHashMap<String, Long> members = new ConcurrentHashMap<>();
    public ChatOutboundAuthorizationInterceptor(GetChatRoomQuery rooms) { this.rooms = rooms; }
    void register(String sessionId, Long memberId) { members.put(sessionId, memberId); }
    @EventListener
    public void disconnected(SessionDisconnectEvent event) { members.remove(event.getSessionId()); }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        if (SimpMessageHeaderAccessor.getMessageType(message.getHeaders()) != SimpMessageType.MESSAGE) return message;
        String session = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
        Long member = session == null ? null : members.get(session);
        Long room = ChatSubscriptionInterceptor.topicRoom(SimpMessageHeaderAccessor.getDestination(message.getHeaders()));
        if (member == null || room == null) return null;
        // A failed authorization lookup must never fall back to an old subscription.
        try { return rooms.isMemberOfRoom(room, member) ? message : null; }
        catch (RuntimeException e) { return null; }
    }
}
