package me.yeonjae.ovlo.domain.board.model;

import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoardTest {

    private final MemberId creator = new MemberId(1L);
    private final UniversityId universityId = new UniversityId(10L);

    @Nested
    @DisplayName("Board.create()")
    class Create {

        @Test
        @DisplayName("GLOBAL 범위 게시판을 생성할 수 있다")
        void shouldCreate_globalBoard() {
            Board board = Board.create("자유게시판", "아무 얘기나", BoardCategory.GENERAL,
                    LocationScope.GLOBAL, creator, null);

            assertThat(board.getName()).isEqualTo("자유게시판");
            assertThat(board.getCategory()).isEqualTo(BoardCategory.GENERAL);
            assertThat(board.getScope()).isEqualTo(LocationScope.GLOBAL);
            assertThat(board.getCreatorId()).isEqualTo(creator);
            assertThat(board.isActive()).isTrue();
        }

        @Test
        @DisplayName("UNIVERSITY 범위 게시판 생성 시 universityId가 포함된다")
        void shouldCreate_universityBoard() {
            Board board = Board.create("서울대 자유게시판", null, BoardCategory.GENERAL,
                    LocationScope.UNIVERSITY, creator, universityId);

            assertThat(board.getUniversityId()).isEqualTo(universityId);
        }

        @Test
        @DisplayName("UNIVERSITY 범위인데 universityId가 null이면 예외가 발생한다")
        void shouldThrow_whenUniversityScopeWithoutUniversityId() {
            assertThatThrownBy(() -> Board.create("게시판", null, BoardCategory.GENERAL,
                    LocationScope.UNIVERSITY, creator, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("UNIVERSITY 범위 게시판은 대학 ID가 필수입니다");
        }

        @Test
        @DisplayName("이름이 null이면 예외가 발생한다")
        void shouldThrow_whenNameIsNull() {
            assertThatThrownBy(() -> Board.create(null, null, BoardCategory.GENERAL,
                    LocationScope.GLOBAL, creator, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("이름이 빈 값이면 예외가 발생한다")
        void shouldThrow_whenNameIsBlank() {
            assertThatThrownBy(() -> Board.create("  ", null, BoardCategory.GENERAL,
                    LocationScope.GLOBAL, creator, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("게시판 이름은 빈 값일 수 없습니다");
        }

        @Test
        @DisplayName("category가 null이면 예외가 발생한다")
        void shouldThrow_whenCategoryIsNull() {
            assertThatThrownBy(() -> Board.create("게시판", null, null,
                    LocationScope.GLOBAL, creator, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("scope가 null이면 예외가 발생한다")
        void shouldThrow_whenScopeIsNull() {
            assertThatThrownBy(() -> Board.create("게시판", null, BoardCategory.GENERAL,
                    null, creator, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("creatorId가 null이면 예외가 발생한다")
        void shouldThrow_whenCreatorIdIsNull() {
            assertThatThrownBy(() -> Board.create("게시판", null, BoardCategory.GENERAL,
                    LocationScope.GLOBAL, null, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Board.deactivate()")
    class Deactivate {

        @Test
        @DisplayName("활성 게시판을 비활성화할 수 있다")
        void shouldDeactivate_activeBoard() {
            Board board = Board.create("게시판", null, BoardCategory.GENERAL,
                    LocationScope.GLOBAL, creator, null);

            board.deactivate();

            assertThat(board.isActive()).isFalse();
        }

        @Test
        @DisplayName("이미 비활성화된 게시판은 다시 비활성화할 수 없다")
        void shouldThrow_whenAlreadyInactive() {
            Board board = Board.create("게시판", null, BoardCategory.GENERAL,
                    LocationScope.GLOBAL, creator, null);
            board.deactivate();

            assertThatThrownBy(board::deactivate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 비활성화된 게시판입니다");
        }
    }

    @Nested
    @DisplayName("Board.restore()")
    class Restore {

        @Test
        @DisplayName("DB에서 비활성 상태 게시판을 복원할 수 있다")
        void shouldRestore_inactiveBoard() {
            Board board = Board.restore(new BoardId(99L), "오래된 게시판", null,
                    BoardCategory.GENERAL, LocationScope.COUNTRY, creator, null, false);

            assertThat(board.getId()).isEqualTo(new BoardId(99L));
            assertThat(board.isActive()).isFalse();
        }
    }
}
