package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import me.yeonjae.ovlo.adapter.in.web.dto.request.CreateChatRoomRequest;
import me.yeonjae.ovlo.application.dto.command.CreateChatRoomCommand;
import me.yeonjae.ovlo.application.dto.result.ChatRoomResult;
import me.yeonjae.ovlo.application.port.in.chat.CreateChatRoomUseCase;
import me.yeonjae.ovlo.application.port.in.chat.GetChatRoomQuery;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Chat", description = "채팅 API")
@RestController
@RequestMapping("/api/v1/chat/rooms")
public class ChatApiController {

    private final CreateChatRoomUseCase createChatRoomUseCase;
    private final GetChatRoomQuery getChatRoomQuery;

    public ChatApiController(
            CreateChatRoomUseCase createChatRoomUseCase,
            GetChatRoomQuery getChatRoomQuery
    ) {
        this.createChatRoomUseCase = createChatRoomUseCase;
        this.getChatRoomQuery = getChatRoomQuery;
    }

    @Operation(summary = "내 채팅방 목록 조회")
    @GetMapping
    public ResponseEntity<List<ChatRoomResult>> getMyChatRooms(
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(getChatRoomQuery.getChatRooms(memberId));
    }

    @Operation(summary = "채팅방 생성")
    @PostMapping
    public ResponseEntity<ChatRoomResult> createRoom(
            @Valid @RequestBody CreateChatRoomRequest request,
            @AuthenticationPrincipal Long memberId
    ) {
        ChatRoomType type = ChatRoomType.valueOf(request.type());
        ChatRoomResult result = createChatRoomUseCase.createChatRoom(
                new CreateChatRoomCommand(type, request.name(), request.participantIds())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "채팅방 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ChatRoomResult> getById(@PathVariable Long id) {
        ChatRoomResult result = getChatRoomQuery.getChatRoom(id);
        return ResponseEntity.ok(result);
    }
}
