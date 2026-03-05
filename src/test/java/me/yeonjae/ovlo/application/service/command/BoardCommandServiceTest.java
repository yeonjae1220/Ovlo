package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.CreateBoardCommand;
import me.yeonjae.ovlo.application.dto.command.SubscribeBoardCommand;
import me.yeonjae.ovlo.application.dto.result.BoardResult;
import me.yeonjae.ovlo.application.port.out.board.LoadBoardPort;
import me.yeonjae.ovlo.application.port.out.board.SaveBoardPort;
import me.yeonjae.ovlo.domain.board.exception.BoardException;
import me.yeonjae.ovlo.domain.board.model.*;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BoardCommandServiceTest {

    @Mock LoadBoardPort loadBoardPort;
    @Mock SaveBoardPort saveBoardPort;

    @InjectMocks
    BoardCommandService service;

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("게시판을 생성하면 저장된 결과를 반환한다")
        void shouldCreate_board() {
            CreateBoardCommand command = new CreateBoardCommand(
                    "자유게시판", "아무 얘기나", "GENERAL", "GLOBAL", 1L, null);

            Board saved = Board.restore(new BoardId(10L), "자유게시판", "아무 얘기나",
                    BoardCategory.GENERAL, LocationScope.GLOBAL, new MemberId(1L), null, true);
            given(saveBoardPort.save(any())).willReturn(saved);

            BoardResult result = service.create(command);

            assertThat(result.id()).isEqualTo(10L);
            assertThat(result.name()).isEqualTo("자유게시판");
            assertThat(result.scope()).isEqualTo("GLOBAL");
            assertThat(result.active()).isTrue();
        }

        @Test
        @DisplayName("UNIVERSITY 범위 게시판을 생성할 수 있다")
        void shouldCreate_universityBoard() {
            CreateBoardCommand command = new CreateBoardCommand(
                    "서울대 게시판", null, "GENERAL", "UNIVERSITY", 1L, 10L);

            Board saved = Board.restore(new BoardId(11L), "서울대 게시판", null,
                    BoardCategory.GENERAL, LocationScope.UNIVERSITY,
                    new MemberId(1L), new me.yeonjae.ovlo.domain.university.model.UniversityId(10L), true);
            given(saveBoardPort.save(any())).willReturn(saved);

            BoardResult result = service.create(command);

            assertThat(result.universityId()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("subscribe()")
    class Subscribe {

        @Test
        @DisplayName("게시판을 구독할 수 있다")
        void shouldSubscribe() {
            BoardId boardId = new BoardId(1L);
            MemberId memberId = new MemberId(2L);
            Board board = Board.restore(boardId, "게시판", null, BoardCategory.GENERAL,
                    LocationScope.GLOBAL, memberId, null, true);

            given(loadBoardPort.findById(boardId)).willReturn(Optional.of(board));
            given(loadBoardPort.existsSubscription(boardId, memberId)).willReturn(false);
            given(saveBoardPort.saveSubscription(any())).willReturn(BoardSubscription.create(boardId, memberId));

            service.subscribe(new SubscribeBoardCommand(1L, 2L));

            verify(saveBoardPort).saveSubscription(any());
        }

        @Test
        @DisplayName("존재하지 않는 게시판 구독 시 예외가 발생한다")
        void shouldThrow_whenBoardNotFound() {
            given(loadBoardPort.findById(any())).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.subscribe(new SubscribeBoardCommand(999L, 1L)))
                    .isInstanceOf(BoardException.class)
                    .hasMessageContaining("게시판을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("이미 구독 중인 게시판은 다시 구독할 수 없다")
        void shouldThrow_whenAlreadySubscribed() {
            BoardId boardId = new BoardId(1L);
            MemberId memberId = new MemberId(2L);
            Board board = Board.restore(boardId, "게시판", null, BoardCategory.GENERAL,
                    LocationScope.GLOBAL, memberId, null, true);

            given(loadBoardPort.findById(boardId)).willReturn(Optional.of(board));
            given(loadBoardPort.existsSubscription(boardId, memberId)).willReturn(true);

            assertThatThrownBy(() -> service.subscribe(new SubscribeBoardCommand(1L, 2L)))
                    .isInstanceOf(BoardException.class)
                    .hasMessageContaining("이미 구독 중인 게시판입니다");
        }
    }

    @Nested
    @DisplayName("unsubscribe()")
    class Unsubscribe {

        @Test
        @DisplayName("구독을 취소할 수 있다")
        void shouldUnsubscribe() {
            BoardId boardId = new BoardId(1L);
            MemberId memberId = new MemberId(2L);
            BoardSubscription subscription = BoardSubscription.create(boardId, memberId);

            given(loadBoardPort.findSubscription(boardId, memberId)).willReturn(Optional.of(subscription));

            service.unsubscribe(new SubscribeBoardCommand(1L, 2L));

            verify(saveBoardPort).deleteSubscription(subscription);
        }

        @Test
        @DisplayName("구독하지 않은 게시판 구독 취소 시 예외가 발생한다")
        void shouldThrow_whenNotSubscribed() {
            given(loadBoardPort.findSubscription(any(), any())).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.unsubscribe(new SubscribeBoardCommand(1L, 2L)))
                    .isInstanceOf(BoardException.class)
                    .hasMessageContaining("구독 정보를 찾을 수 없습니다");
        }
    }
}
