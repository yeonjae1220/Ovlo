package me.yeonjae.ovlo.domain.board.model;

import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoardSubscriptionTest {

    @Test
    @DisplayName("게시판 구독을 생성할 수 있다")
    void shouldCreate_subscription() {
        BoardId boardId = new BoardId(1L);
        MemberId memberId = new MemberId(2L);

        BoardSubscription subscription = BoardSubscription.create(boardId, memberId);

        assertThat(subscription.getBoardId()).isEqualTo(boardId);
        assertThat(subscription.getMemberId()).isEqualTo(memberId);
    }

    @Test
    @DisplayName("boardId가 null이면 예외가 발생한다")
    void shouldThrow_whenBoardIdIsNull() {
        assertThatThrownBy(() -> BoardSubscription.create(null, new MemberId(1L)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("memberId가 null이면 예외가 발생한다")
    void shouldThrow_whenMemberIdIsNull() {
        assertThatThrownBy(() -> BoardSubscription.create(new BoardId(1L), null))
                .isInstanceOf(NullPointerException.class);
    }
}
