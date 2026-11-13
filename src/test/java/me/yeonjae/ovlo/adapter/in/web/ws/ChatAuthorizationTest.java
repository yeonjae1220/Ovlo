package me.yeonjae.ovlo.adapter.in.web.ws;

import me.yeonjae.ovlo.application.port.in.chat.GetChatRoomQuery;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.*;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatAuthorizationTest {
    GetChatRoomQuery rooms = mock(GetChatRoomQuery.class);
    ChatOutboundAuthorizationInterceptor outbound = new ChatOutboundAuthorizationInterceptor(rooms);
    ChatSubscriptionInterceptor inbound = new ChatSubscriptionInterceptor(rooms, outbound);

    Message<byte[]> frame(StompCommand command, String destination) {
        var h = StompHeaderAccessor.create(command);
        h.setSessionId("synthetic-session");
        h.setSessionAttributes(new HashMap<>(Map.of(WsSessionKeys.MEMBER_ID, 20L)));
        if (command == StompCommand.SUBSCRIBE) h.setSubscriptionId("subscription");
        if (destination != null) h.setDestination(destination);
        h.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], h.getMessageHeaders());
    }

    Message<byte[]> delivery(String destination) {
        var h = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        h.setSessionId("synthetic-session");
        h.setDestination(destination);
        return MessageBuilder.createMessage(new byte[0], h.getMessageHeaders());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/topic/chat/*", "/topic/**", "/topic/chat/{room}", "/topic/chat/7/**",
        "/topic/chat/7/", "/topic/chat/0", "/queue/anything", "/topic/chat/999999999999999999999999"})
    void rejectsPatternsUnknownDestinationsAndOverflow(String destination) {
        assertThatThrownBy(() -> inbound.preSend(frame(StompCommand.SUBSCRIBE, destination), null))
            .isInstanceOf(MessageDeliveryException.class);
        verifyNoInteractions(rooms);
    }

    @Test void rejectsUnauthorizedExactSubscriptionAndMissingIdentity() {
        assertThatThrownBy(() -> inbound.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chat/7"), null))
            .isInstanceOf(MessageDeliveryException.class);
        var message = frame(StompCommand.SUBSCRIBE, "/topic/chat/7");
        StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class).getSessionAttributes().clear();
        assertThatThrownBy(() -> inbound.preSend(message, null)).isInstanceOf(MessageDeliveryException.class);
    }

    @Test void brokerDestinationsCannotBePublishedByClients() {
        when(rooms.isMemberOfRoom(7L, 20L)).thenReturn(true);
        assertThat(inbound.preSend(frame(StompCommand.SEND, "/app/chat/7"), null)).isNotNull();
        for (String target : List.of("/topic/chat/7", "/topic/chat/7/read", "/queue/anything", "/app/chat/*")) {
            assertThatThrownBy(() -> inbound.preSend(frame(StompCommand.SEND, target), null))
                .isInstanceOf(MessageDeliveryException.class);
        }
        when(rooms.isMemberOfRoom(7L, 20L)).thenReturn(false);
        assertThatThrownBy(() -> inbound.preSend(frame(StompCommand.SEND, "/app/chat/7"), null))
            .isInstanceOf(MessageDeliveryException.class);
    }

    @Test void realBrokerStopsDeliveringOnRevocationWithoutReconnect() {
        var clientIn = new ExecutorSubscribableChannel();
        var clientOut = new ExecutorSubscribableChannel();
        var server = new ExecutorSubscribableChannel();
        clientIn.addInterceptor(inbound); clientOut.addInterceptor(outbound);
        List<Message<?>> received = new ArrayList<>();
        clientOut.subscribe(received::add);
        var broker = new SimpleBrokerMessageHandler(clientIn, clientOut, server, List.of("/topic", "/queue"));
        broker.start();
        try {
            clientIn.send(frame(StompCommand.CONNECT, null));
            received.clear();
            when(rooms.isMemberOfRoom(7L, 20L)).thenReturn(true);
            clientIn.send(frame(StompCommand.SUBSCRIBE, "/topic/chat/7"));
            server.send(delivery("/topic/chat/7"));
            assertThat(received).hasSize(1);
            received.clear();
            when(rooms.isMemberOfRoom(7L, 20L)).thenReturn(false);
            server.send(delivery("/topic/chat/7"));
            assertThat(received).isEmpty();
            assertThatThrownBy(() -> clientIn.send(frame(StompCommand.SUBSCRIBE, "/topic/chat/*")))
                .isInstanceOf(MessageDeliveryException.class);
            server.send(delivery("/topic/chat/99"));
            assertThat(received).isEmpty();
        } finally { broker.stop(); }
    }

    @Test void readEventsDisconnectAndLookupFailureFailClosed() {
        when(rooms.isMemberOfRoom(7L, 20L)).thenReturn(true);
        inbound.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chat/7/read"), null);
        assertThat(outbound.preSend(delivery("/topic/chat/7/read"), null)).isNotNull();
        when(rooms.isMemberOfRoom(7L, 20L)).thenThrow(new IllegalStateException("DB unavailable"));
        assertThat(outbound.preSend(delivery("/topic/chat/7/read"), null)).isNull();
        outbound.disconnected(new SessionDisconnectEvent(this, frame(StompCommand.DISCONNECT, null),
            "synthetic-session", CloseStatus.NORMAL));
        assertThat(outbound.preSend(delivery("/topic/chat/7/read"), null)).isNull();
    }
}
