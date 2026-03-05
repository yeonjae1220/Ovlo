package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.ChatRoomResult;
import me.yeonjae.ovlo.application.port.in.chat.GetChatRoomQuery;
import me.yeonjae.ovlo.application.port.out.chat.LoadChatPort;
import me.yeonjae.ovlo.domain.chat.exception.ChatException;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomId;
import org.springframework.stereotype.Service;

@Service
public class ChatQueryService implements GetChatRoomQuery {

    private final LoadChatPort loadChatPort;

    public ChatQueryService(LoadChatPort loadChatPort) {
        this.loadChatPort = loadChatPort;
    }

    @Override
    public ChatRoomResult getChatRoom(Long chatRoomId) {
        return loadChatPort.findById(new ChatRoomId(chatRoomId))
                .map(ChatRoomResult::from)
                .orElseThrow(() -> new ChatException("채팅방을 찾을 수 없습니다"));
    }
}
