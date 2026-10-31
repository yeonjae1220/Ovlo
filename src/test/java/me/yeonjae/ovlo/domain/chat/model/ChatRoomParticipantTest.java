package me.yeonjae.ovlo.domain.chat.model;

import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 상담방(GROUP)에 담당자를 넣고 빼는 규칙. DM 은 2명 고정. */
class ChatRoomParticipantTest {

    private static final MemberId STUDENT = new MemberId(42L);
    private static final MemberId STAFF_A = new MemberId(11L);
    private static final MemberId STAFF_B = new MemberId(12L);

    @Test
    @DisplayName("GROUP 방에 참여자를 더하고 뺄 수 있고, 이미 있거나 없는 회원은 무시한다")
    void addAndRemove() {
        ChatRoom room = ChatRoom.create(ChatRoomType.GROUP, "상담", List.of(STUDENT, STAFF_A));

        assertThat(room.addParticipant(STAFF_B)).isTrue();
        assertThat(room.addParticipant(STAFF_B)).isFalse();
        assertThat(room.hasParticipant(STAFF_B)).isTrue();

        assertThat(room.removeParticipant(STAFF_A)).isTrue();
        assertThat(room.removeParticipant(STAFF_A)).isFalse();
        assertThat(room.getParticipants()).containsExactly(STUDENT, STAFF_B);
    }

    @Test
    @DisplayName("2명 미만이 되는 제거와 DM 변경은 거부한다")
    void guards() {
        ChatRoom group = ChatRoom.create(ChatRoomType.GROUP, "상담", List.of(STUDENT, STAFF_A));
        assertThatThrownBy(() -> group.removeParticipant(STAFF_A)).isInstanceOf(IllegalStateException.class);

        ChatRoom dm = ChatRoom.create(ChatRoomType.DM, null, List.of(STUDENT, STAFF_A));
        assertThatThrownBy(() -> dm.addParticipant(STAFF_B)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> dm.removeParticipant(STAFF_A)).isInstanceOf(IllegalStateException.class);
    }
}
