package me.yeonjae.ovlo.adapter.in.web.ws;

import me.yeonjae.ovlo.application.dto.command.SendMessageCommand;
import me.yeonjae.ovlo.application.dto.result.MessageResult;
import me.yeonjae.ovlo.application.port.in.chat.SendMessageUseCase;
import me.yeonjae.ovlo.domain.chat.exception.ChatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ChatWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    /** 클라이언트가 구독하는 주소. 서버 쪽에서는 세션마다 {@link #errorQueueOf(String)} 로 풀린다. */
    public static final String ERROR_SUBSCRIPTION = "/user/queue/errors";
    private static final int MAX_CLIENT_MESSAGE_ID_LENGTH = 64;

    private final SendMessageUseCase sendMessageUseCase;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketHandler(SendMessageUseCase sendMessageUseCase,
                                SimpMessagingTemplate messagingTemplate) {
        this.sendMessageUseCase = sendMessageUseCase;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Principal 없이 세션 단위로 연결하므로, Spring 의 사용자 목적지 해석이
     * {@code /user/queue/errors} 구독을 바꿔 놓는 세션 전용 브로커 주소로 직접 보낸다.
     */
    static String errorQueueOf(String sessionId) {
        return "/queue/errors-user" + sessionId;
    }

    @MessageMapping("/chat/{roomId}")
    public void handleMessage(
            @DestinationVariable Long roomId,
            ChatMessagePayload payload,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Long memberId = (Long) headerAccessor.getSessionAttributes().get(WsSessionKeys.MEMBER_ID);
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다");
        }
        MessageResult result;
        try {
            result = sendMessageUseCase.sendMessage(new SendMessageCommand(roomId, memberId, payload.content()));
        } catch (ChatException e) {
            // 차단된 DM 등 거절 사유를 보낸 사람에게만 알린다. 예외를 그냥 던지면 STOMP 로는 아무것도 돌아가지 않는다.
            reportFailure(headerAccessor.getSessionId(), roomId, payload, e.getErrorType().name(), e.getMessage());
            return;
        } catch (RuntimeException e) {
            log.error("STOMP 메시지 전송 실패 roomId={} memberId={}", roomId, memberId, e);
            reportFailure(headerAccessor.getSessionId(), roomId, payload, "SEND_FAILED", "메시지를 보내지 못했습니다");
            return;
        }
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, result);
    }

    private void reportFailure(String sessionId, Long roomId, ChatMessagePayload payload, String code, String message) {
        if (sessionId == null) {
            return;
        }
        messagingTemplate.convertAndSend(errorQueueOf(sessionId),
                new ChatSendError(roomId, payload.safeClientMessageId(), code, message));
    }

    /** @param clientMessageId 클라이언트가 붙인 임시 ID. 실패 알림에 그대로 돌려줘 어떤 전송이 실패했는지 맞춘다. */
    public record ChatMessagePayload(String content, String clientMessageId) {
        String safeClientMessageId() {
            if (clientMessageId == null || clientMessageId.isBlank()
                    || clientMessageId.length() > MAX_CLIENT_MESSAGE_ID_LENGTH) {
                return null;
            }
            return clientMessageId;
        }
    }

    public record ChatSendError(Long roomId, String clientMessageId, String code, String message) {}
}
