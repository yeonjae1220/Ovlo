package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.CreateChatRoomCommand;
import me.yeonjae.ovlo.application.dto.command.SendMessageCommand;
import me.yeonjae.ovlo.application.dto.result.ChatRoomResult;
import me.yeonjae.ovlo.application.dto.result.MessageResult;
import me.yeonjae.ovlo.application.port.out.chat.LoadChatPort;
import me.yeonjae.ovlo.application.port.out.chat.SaveChatPort;
import me.yeonjae.ovlo.application.port.out.chat.SaveReadMarkerPort;
import me.yeonjae.ovlo.domain.chat.exception.ChatException;
import me.yeonjae.ovlo.domain.chat.model.ChatRoom;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomId;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomType;
import me.yeonjae.ovlo.domain.chat.model.Message;
import me.yeonjae.ovlo.domain.chat.model.MessageId;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatCommandServiceTest {

    @Mock LoadChatPort loadChatPort;
    @Mock SaveChatPort saveChatPort;
    @Mock SaveReadMarkerPort saveReadMarkerPort;

    @InjectMocks
    ChatCommandService service;

    @Nested
    @DisplayName("createChatRoom()")
    class CreateChatRoom {

        @Test
        @DisplayName("채팅방을 생성할 수 있다")
        void shouldCreateChatRoom() {
            CreateChatRoomCommand command = new CreateChatRoomCommand(
                    ChatRoomType.GROUP, "테스트방", List.of(1L, 2L, 3L));
            ChatRoom saved = ChatRoom.restore(new ChatRoomId(1L), ChatRoomType.GROUP, "테스트방",
                    List.of(new MemberId(1L), new MemberId(2L), new MemberId(3L)), List.of());
            given(saveChatPort.save(any())).willReturn(saved);

            ChatRoomResult result = service.createChatRoom(command);

            assertThat(result.chatRoomId()).isEqualTo(1L);
            assertThat(result.type()).isEqualTo(ChatRoomType.GROUP);
            verify(saveChatPort).save(any());
        }

        @Test
        @DisplayName("이미 DM 방이 존재하면 기존 방을 반환한다")
        void shouldReturnExisting_whenDmRoomAlreadyExists() {
            CreateChatRoomCommand command = new CreateChatRoomCommand(
                    ChatRoomType.DM, null, List.of(1L, 2L));
            ChatRoomId existingId = new ChatRoomId(42L);
            ChatRoom existingRoom = ChatRoom.restore(existingId, ChatRoomType.DM, null,
                    List.of(new MemberId(1L), new MemberId(2L)), List.of());
            given(loadChatPort.findDmRoomId(any(), any())).willReturn(Optional.of(existingId));
            given(loadChatPort.findById(existingId)).willReturn(Optional.of(existingRoom));

            ChatRoomResult result = service.createChatRoom(command);

            assertThat(result.chatRoomId()).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("sendMessage()")
    class SendMessage {

        @Test
        @DisplayName("채팅방에 메시지를 전송할 수 있다")
        void shouldSendMessage() {
            Message savedMsg = Message.restore(
                    new MessageId(10L), new MemberId(1L), "안녕하세요", Instant.now());
            given(loadChatPort.isMember(any(), any())).willReturn(true);
            given(saveChatPort.saveMessage(any(), any(), any())).willReturn(savedMsg);

            SendMessageCommand command = new SendMessageCommand(1L, 1L, "안녕하세요");
            MessageResult result = service.sendMessage(command);

            assertThat(result.messageId()).isEqualTo(10L);
            assertThat(result.content()).isEqualTo("안녕하세요");
            assertThat(result.senderId()).isEqualTo(1L);
            verify(saveChatPort).saveMessage(1L, 1L, "안녕하세요");
        }

        @Test
        @DisplayName("채팅방 참여자가 아니면 예외가 발생한다")
        void shouldThrow_whenNotMember() {
            given(loadChatPort.isMember(any(), any())).willReturn(false);

            assertThatThrownBy(() -> service.sendMessage(new SendMessageCommand(99L, 1L, "hi")))
                    .isInstanceOf(ChatException.class)
                    .hasMessageContaining("참여자가 아닙니다");
        }
    }
}
