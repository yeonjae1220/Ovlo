package me.yeonjae.ovlo.adapter.in.web.ws;

import me.yeonjae.ovlo.application.dto.command.SendMessageCommand;
import me.yeonjae.ovlo.application.dto.result.MessageResult;
import me.yeonjae.ovlo.application.port.in.chat.SendMessageUseCase;
import me.yeonjae.ovlo.domain.chat.exception.ChatException;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ChatWebSocketHandlerTest {

    SendMessageUseCase useCase = mock(SendMessageUseCase.class);
    SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
    ChatWebSocketHandler handler = new ChatWebSocketHandler(useCase, template);

    SimpMessageHeaderAccessor headers(String sessionId) {
        var h = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        h.setSessionId(sessionId);
        h.setSessionAttributes(new HashMap<>(Map.of(WsSessionKeys.MEMBER_ID, 20L)));
        return h;
    }

    @Test
    void rejectedSendIsReportedOnlyToTheSendersSessionWithItsClientId() {
        given(useCase.sendMessage(new SendMessageCommand(7L, 20L, "hi")))
                .willThrow(new ChatException("상대와 메시지를 주고받을 수 없습니다", ChatException.ErrorType.FORBIDDEN));

        handler.handleMessage(7L, new ChatWebSocketHandler.ChatMessagePayload("hi", "c-1"), headers("s1"));

        verify(template).convertAndSend("/queue/errors-users1",
                new ChatWebSocketHandler.ChatSendError(7L, "c-1", "FORBIDDEN", "상대와 메시지를 주고받을 수 없습니다"));
        verify(template, never()).convertAndSend(eq("/topic/chat/7"), any(Object.class));
    }

    @Test
    void unexpectedFailureIsReportedWithoutInternalDetail() {
        given(useCase.sendMessage(any())).willThrow(new IllegalStateException("connection refused: db-1"));

        handler.handleMessage(7L, new ChatWebSocketHandler.ChatMessagePayload("hi", "c-2"), headers("s1"));

        verify(template).convertAndSend("/queue/errors-users1",
                new ChatWebSocketHandler.ChatSendError(7L, "c-2", "SEND_FAILED", "메시지를 보내지 못했습니다"));
    }

    @Test
    void oversizedOrMissingClientIdIsNotEchoed() {
        given(useCase.sendMessage(any())).willThrow(new ChatException("x", ChatException.ErrorType.BAD_REQUEST));

        handler.handleMessage(7L, new ChatWebSocketHandler.ChatMessagePayload("hi", "a".repeat(65)), headers("s1"));
        handler.handleMessage(7L, new ChatWebSocketHandler.ChatMessagePayload("hi", null), headers("s1"));

        verify(template, org.mockito.Mockito.times(2)).convertAndSend("/queue/errors-users1",
                new ChatWebSocketHandler.ChatSendError(7L, null, "BAD_REQUEST", "x"));
    }

    @Test
    void successfulSendIsBroadcastToTheRoomOnly() {
        MessageResult result = mock(MessageResult.class);
        given(useCase.sendMessage(any())).willReturn(result);

        handler.handleMessage(7L, new ChatWebSocketHandler.ChatMessagePayload("hi", "c-3"), headers("s1"));

        verify(template).convertAndSend("/topic/chat/7", (Object) result);
        verify(template, never()).convertAndSend(eq("/queue/errors-users1"), any(Object.class));
    }

    @Test
    void failureWithoutSessionSendsNothing() {
        given(useCase.sendMessage(any())).willThrow(new ChatException("x", ChatException.ErrorType.FORBIDDEN));

        handler.handleMessage(7L, new ChatWebSocketHandler.ChatMessagePayload("hi", "c-4"), headers(null));

        verifyNoInteractions(template);
    }
}
