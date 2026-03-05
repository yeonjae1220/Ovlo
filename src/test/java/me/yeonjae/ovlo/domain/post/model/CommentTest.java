package me.yeonjae.ovlo.domain.post.model;

import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentTest {

    private final MemberId authorId = new MemberId(1L);

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("댓글을 생성할 수 있다")
        void shouldCreate_comment() {
            Comment comment = Comment.create(authorId, "댓글 내용");

            assertThat(comment.getContent()).isEqualTo("댓글 내용");
            assertThat(comment.getAuthorId()).isEqualTo(authorId);
            assertThat(comment.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("댓글 내용이 null이면 예외가 발생한다")
        void shouldThrow_whenContentIsNull() {
            assertThatThrownBy(() -> Comment.create(authorId, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("댓글 내용은 필수입니다");
        }

        @Test
        @DisplayName("댓글 내용이 공백이면 예외가 발생한다")
        void shouldThrow_whenContentIsBlank() {
            assertThatThrownBy(() -> Comment.create(authorId, "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("댓글 내용은 빈 값일 수 없습니다");
        }

        @Test
        @DisplayName("작성자 ID가 null이면 예외가 발생한다")
        void shouldThrow_whenAuthorIdIsNull() {
            assertThatThrownBy(() -> Comment.create(null, "내용"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("댓글 작성자 ID는 필수입니다");
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("댓글을 삭제할 수 있다")
        void shouldDelete_comment() {
            Comment comment = Comment.create(authorId, "내용");
            comment.delete();

            assertThat(comment.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("이미 삭제된 댓글을 다시 삭제하면 예외가 발생한다")
        void shouldThrow_whenAlreadyDeleted() {
            Comment comment = Comment.create(authorId, "내용");
            comment.delete();

            assertThatThrownBy(() -> comment.delete())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 삭제된 댓글입니다");
        }
    }
}
