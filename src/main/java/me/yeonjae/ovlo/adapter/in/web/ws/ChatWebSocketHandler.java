package me.yeonjae.ovlo.adapter.in.web.ws;

import me.yeonjae.ovlo.application.dto.command.SendMessageCommand;
import me.yeonjae.ovlo.application.dto.result.MessageResult;
import me.yeonjae.ovlo.application.port.in.chat.SendMessageUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ChatWebSocketHandler {

    private final SendMessageUseCase sendMessageUseCase;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketHandler(SendMessageUseCase sendMessageUseCase,
                                SimpMessagingTemplate messagingTemplate) {
        this.sendMessageUseCase = sendMessageUseCase;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat/{roomId}")
    public void handleMessage(
            @DestinationVariable Long roomId,
            ChatMessagePayload payload,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Long memberId = (Long) headerAccessor.getSessionAttributes().get("memberId");
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다");
        }
        SendMessageCommand command = new SendMessageCommand(roomId, memberId, payload.content());
        MessageResult result = sendMessageUseCase.sendMessage(command);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, result);
    }

    public record ChatMessagePayload(String content) {}
}
