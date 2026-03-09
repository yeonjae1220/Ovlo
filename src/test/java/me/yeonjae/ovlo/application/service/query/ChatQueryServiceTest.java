package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.ChatRoomResult;
import me.yeonjae.ovlo.application.port.out.chat.LoadChatPort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.domain.chat.exception.ChatException;
import me.yeonjae.ovlo.domain.chat.model.ChatRoom;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomId;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomType;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatQueryServiceTest {

    @Mock LoadChatPort loadChatPort;
    @Mock LoadMemberPort loadMemberPort;

    @InjectMocks
    ChatQueryService service;

    @Test
    @DisplayName("채팅방 ID로 채팅방을 조회할 수 있다")
    void shouldGetChatRoom() {
        ChatRoom room = ChatRoom.restore(new ChatRoomId(1L), ChatRoomType.DM, null,
                List.of(new MemberId(1L), new MemberId(2L)), List.of());
        given(loadChatPort.findById(any())).willReturn(Optional.of(room));

        ChatRoomResult result = service.getChatRoom(1L);

        assertThat(result.chatRoomId()).isEqualTo(1L);
        assertThat(result.type()).isEqualTo(ChatRoomType.DM);
        assertThat(result.participantIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("존재하지 않는 채팅방을 조회하면 예외가 발생한다")
    void shouldThrow_whenChatRoomNotFound() {
        given(loadChatPort.findById(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getChatRoom(99L))
                .isInstanceOf(ChatException.class)
                .hasMessageContaining("채팅방을 찾을 수 없습니다");
    }
}
