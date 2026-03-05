package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.command.SearchBoardCommand;
import me.yeonjae.ovlo.application.dto.result.BoardPageResult;
import me.yeonjae.ovlo.application.dto.result.BoardResult;
import me.yeonjae.ovlo.application.port.out.board.LoadBoardPort;
import me.yeonjae.ovlo.application.port.out.board.SearchBoardPort;
import me.yeonjae.ovlo.domain.board.exception.BoardException;
import me.yeonjae.ovlo.domain.board.model.*;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BoardQueryServiceTest {

    @Mock LoadBoardPort loadBoardPort;
    @Mock SearchBoardPort searchBoardPort;

    @InjectMocks
    BoardQueryService service;

    @Test
    @DisplayName("키워드로 게시판을 검색할 수 있다")
    void shouldSearch_byKeyword() {
        Board board = Board.restore(new BoardId(1L), "자유게시판", null, BoardCategory.GENERAL,
                LocationScope.GLOBAL, new MemberId(1L), null, true);

        given(searchBoardPort.search(eq("자유"), isNull(), isNull(), eq(0), eq(10)))
                .willReturn(List.of(board));
        given(searchBoardPort.count(eq("자유"), isNull(), isNull())).willReturn(1L);

        BoardPageResult result = service.search(new SearchBoardCommand("자유", null, null, 0, 10));

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.content().get(0).name()).isEqualTo("자유게시판");
    }

    @Test
    @DisplayName("카테고리로 게시판을 필터링할 수 있다")
    void shouldSearch_byCategory() {
        given(searchBoardPort.search(isNull(), eq(BoardCategory.GENERAL), isNull(), eq(0), eq(10)))
                .willReturn(List.of());
        given(searchBoardPort.count(isNull(), eq(BoardCategory.GENERAL), isNull())).willReturn(0L);

        BoardPageResult result = service.search(new SearchBoardCommand(null, "GENERAL", null, 0, 10));

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0L);
    }

    @Test
    @DisplayName("ID로 게시판을 조회할 수 있다")
    void shouldGetById() {
        BoardId boardId = new BoardId(1L);
        Board board = Board.restore(boardId, "게시판", null, BoardCategory.CULTURE,
                LocationScope.COUNTRY, new MemberId(1L), null, true);

        given(loadBoardPort.findById(boardId)).willReturn(Optional.of(board));

        BoardResult result = service.getById(boardId);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.category()).isEqualTo("CULTURE");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 예외가 발생한다")
    void shouldThrow_whenNotFound() {
        given(loadBoardPort.findById(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(new BoardId(999L)))
                .isInstanceOf(BoardException.class)
                .hasMessageContaining("게시판을 찾을 수 없습니다");
    }
}
