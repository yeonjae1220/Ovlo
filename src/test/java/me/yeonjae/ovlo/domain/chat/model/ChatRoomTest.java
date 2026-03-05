package me.yeonjae.ovlo.domain.chat.model;

import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatRoomTest {

    private final MemberId member1 = new MemberId(1L);
    private final MemberId member2 = new MemberId(2L);
    private final MemberId member3 = new MemberId(3L);

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("DM 채팅방을 생성할 수 있다")
        void shouldCreate_dmRoom() {
            ChatRoom room = ChatRoom.create(ChatRoomType.DM, null, List.of(member1, member2));

            assertThat(room.getType()).isEqualTo(ChatRoomType.DM);
            assertThat(room.getParticipants()).containsExactlyInAnyOrder(member1, member2);
            assertThat(room.getId()).isNull();
        }

        @Test
        @DisplayName("그룹 채팅방을 생성할 수 있다")
        void shouldCreate_groupRoom() {
            ChatRoom room = ChatRoom.create(ChatRoomType.GROUP, "그룹방", List.of(member1, member2, member3));

            assertThat(room.getType()).isEqualTo(ChatRoomType.GROUP);
            assertThat(room.getName()).isEqualTo("그룹방");
            assertThat(room.getParticipants()).hasSize(3);
        }

        @Test
        @DisplayName("참여자가 1명 이하이면 예외가 발생한다")
        void shouldThrow_whenParticipantsTooFew() {
            assertThatThrownBy(() -> ChatRoom.create(ChatRoomType.GROUP, "방", List.of(member1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("최소 2명");
        }

        @Test
        @DisplayName("DM 방에 참여자가 2명이 아니면 예외가 발생한다")
        void shouldThrow_whenDmHasNotExactlyTwoParticipants() {
            assertThatThrownBy(() -> ChatRoom.create(ChatRoomType.DM, null, List.of(member1, member2, member3)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("DM 채팅방은 정확히 2명");
        }

        @Test
        @DisplayName("타입이 null이면 예외가 발생한다")
        void shouldThrow_whenTypeIsNull() {
            assertThatThrownBy(() -> ChatRoom.create(null, "방", List.of(member1, member2)))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("채팅방 타입은 필수입니다");
        }
    }

    @Nested
    @DisplayName("restore()")
    class Restore {

        @Test
        @DisplayName("채팅방을 복원할 수 있다")
        void shouldRestore_chatRoom() {
            ChatRoomId roomId = new ChatRoomId(10L);

            ChatRoom room = ChatRoom.restore(roomId, ChatRoomType.DM, null,
                    List.of(member1, member2), List.of());

            assertThat(room.getId()).isEqualTo(roomId);
            assertThat(room.getType()).isEqualTo(ChatRoomType.DM);
            assertThat(room.getParticipants()).containsExactlyInAnyOrder(member1, member2);
            assertThat(room.getMessages()).isEmpty();
        }
    }

    @Nested
    @DisplayName("addMessage()")
    class AddMessage {

        @Test
        @DisplayName("참여자가 메시지를 보낼 수 있다")
        void shouldAddMessage_byParticipant() {
            ChatRoom room = ChatRoom.create(ChatRoomType.DM, null, List.of(member1, member2));

            Message message = room.addMessage(member1, "안녕하세요");

            assertThat(message.getContent()).isEqualTo("안녕하세요");
            assertThat(message.getSenderId()).isEqualTo(member1);
            assertThat(room.getMessages()).hasSize(1);
        }

        @Test
        @DisplayName("참여자가 아닌 사람이 메시지를 보내면 예외가 발생한다")
        void shouldThrow_whenNonParticipantSendsMessage() {
            ChatRoom room = ChatRoom.create(ChatRoomType.DM, null, List.of(member1, member2));

            assertThatThrownBy(() -> room.addMessage(member3, "안녕하세요"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("채팅방 참여자만");
        }
    }
}
