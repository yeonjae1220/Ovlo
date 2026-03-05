package me.yeonjae.ovlo.domain.post.model;

import me.yeonjae.ovlo.domain.board.model.BoardId;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.post.exception.PostException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostTest {

    private BoardId boardId;
    private MemberId authorId;

    @BeforeEach
    void setUp() {
        boardId = new BoardId(1L);
        authorId = new MemberId(1L);
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("게시글을 생성하면 삭제되지 않은 상태로 시작한다")
        void shouldCreate_withNotDeletedState() {
            Post post = Post.create(boardId, authorId, "제목", "내용");

            assertThat(post.getTitle()).isEqualTo("제목");
            assertThat(post.getContent()).isEqualTo("내용");
            assertThat(post.getBoardId()).isEqualTo(boardId);
            assertThat(post.getAuthorId()).isEqualTo(authorId);
            assertThat(post.isDeleted()).isFalse();
            assertThat(post.getComments()).isEmpty();
            assertThat(post.getReactions()).isEmpty();
        }

        @Test
        @DisplayName("게시판 ID가 null이면 예외가 발생한다")
        void shouldThrow_whenBoardIdIsNull() {
            assertThatThrownBy(() -> Post.create(null, authorId, "제목", "내용"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("게시판 ID는 필수입니다");
        }

        @Test
        @DisplayName("작성자 ID가 null이면 예외가 발생한다")
        void shouldThrow_whenAuthorIdIsNull() {
            assertThatThrownBy(() -> Post.create(boardId, null, "제목", "내용"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("게시글 작성자 ID는 필수입니다");
        }

        @Test
        @DisplayName("제목이 null이면 예외가 발생한다")
        void shouldThrow_whenTitleIsNull() {
            assertThatThrownBy(() -> Post.create(boardId, authorId, null, "내용"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("게시글 제목은 필수입니다");
        }

        @Test
        @DisplayName("제목이 공백이면 예외가 발생한다")
        void shouldThrow_whenTitleIsBlank() {
            assertThatThrownBy(() -> Post.create(boardId, authorId, "  ", "내용"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("게시글 제목은 빈 값일 수 없습니다");
        }

        @Test
        @DisplayName("내용이 null이면 예외가 발생한다")
        void shouldThrow_whenContentIsNull() {
            assertThatThrownBy(() -> Post.create(boardId, authorId, "제목", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("게시글 내용은 필수입니다");
        }

        @Test
        @DisplayName("내용이 공백이면 예외가 발생한다")
        void shouldThrow_whenContentIsBlank() {
            assertThatThrownBy(() -> Post.create(boardId, authorId, "제목", "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("게시글 내용은 빈 값일 수 없습니다");
        }
    }

    @Nested
    @DisplayName("react()")
    class React {

        @Test
        @DisplayName("좋아요를 누를 수 있다")
        void shouldReact_withLike() {
            Post post = Post.create(boardId, authorId, "제목", "내용");
            post.react(authorId, ReactionType.LIKE);

            assertThat(post.likeCount()).isEqualTo(1);
            assertThat(post.dislikeCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("싫어요를 누를 수 있다")
        void shouldReact_withDislike() {
            Post post = Post.create(boardId, authorId, "제목", "내용");
            post.react(authorId, ReactionType.DISLIKE);

            assertThat(post.dislikeCount()).isEqualTo(1);
            assertThat(post.likeCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("이미 동일한 반응을 누르면 예외가 발생한다")
        void shouldThrow_whenDuplicateReaction() {
            Post post = Post.create(boardId, authorId, "제목", "내용");
            post.react(authorId, ReactionType.LIKE);

            assertThatThrownBy(() -> post.react(authorId, ReactionType.LIKE))
                    .isInstanceOf(PostException.class)
                    .hasMessageContaining("이미 동일한 반응을 했습니다");
        }

        @Test
        @DisplayName("좋아요 → 싫어요 전환 시 기존 반응이 제거된다")
        void shouldSwitchReaction_fromLikeToDislike() {
            Post post = Post.create(boardId, authorId, "제목", "내용");
            post.react(authorId, ReactionType.LIKE);
            post.react(authorId, ReactionType.DISLIKE);

            assertThat(post.likeCount()).isEqualTo(0);
            assertThat(post.dislikeCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("삭제된 게시글에는 반응할 수 없다")
        void shouldThrow_whenPostDeleted() {
            Post post = Post.create(boardId, authorId, "제목", "내용");
            post.delete();

            assertThatThrownBy(() -> post.react(authorId, ReactionType.LIKE))
                    .isInstanceOf(PostException.class)
                    .hasMessageContaining("삭제된 게시글에는 반응할 수 없습니다");
        }
    }

    @Nested
    @DisplayName("unreact()")
    class Unreact {

        @Test
        @DisplayName("반응을 취소할 수 있다")
        void shouldUnreact() {
            Post post = Post.create(boardId, authorId, "제목", "내용");
            post.react(authorId, ReactionType.LIKE);
            post.unreact(authorId);

            assertThat(post.likeCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("반응하지 않은 상태에서 취소하면 예외가 발생한다")
        void shouldThrow_whenNoReaction() {
            Post post = Post.create(boardId, authorId, "제목", "내용");

            assertThatThrownBy(() -> post.unreact(authorId))
                    .isInstanceOf(PostException.class)
                    .hasMessageContaining("반응을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("addComment()")
    class AddComment {

        @Test
        @DisplayName("댓글을 추가할 수 있다")
        void shouldAddComment() {
            Post post = Post.create(boardId, authorId, "제목", "내용");
            Comment comment = post.addComment(authorId, "댓글입니다");

            assertThat(post.getComments()).hasSize(1);
            assertThat(comment.getContent()).isEqualTo("댓글입니다");
            assertThat(comment.getAuthorId()).isEqualTo(authorId);
        }

        @Test
        @DisplayName("삭제된 게시글에는 댓글을 달 수 없다")
        void shouldThrow_whenPostDeleted() {
            Post post = Post.create(boardId, authorId, "제목", "내용");
            post.delete();

            assertThatThrownBy(() -> post.addComment(authorId, "댓글"))
                    .isInstanceOf(PostException.class)
                    .hasMessageContaining("삭제된 게시글에는 댓글을 달 수 없습니다");
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("게시글을 삭제할 수 있다")
        void shouldDelete() {
            Post post = Post.create(boardId, authorId, "제목", "내용");
            post.delete();

            assertThat(post.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("이미 삭제된 게시글을 다시 삭제하면 예외가 발생한다")
        void shouldThrow_whenAlreadyDeleted() {
            Post post = Post.create(boardId, authorId, "제목", "내용");
            post.delete();

            assertThatThrownBy(() -> post.delete())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 삭제된 게시글입니다");
        }
    }

    @Nested
    @DisplayName("restore()")
    class Restore {

        @Test
        @DisplayName("DB에서 복원 시 모든 필드를 원상 복구한다")
        void shouldRestore_allFields() {
            MemberId member1 = new MemberId(1L);
            MemberId member2 = new MemberId(2L);
            List<Comment> comments = List.of(
                    Comment.restore(new CommentId(1L), new PostId(10L), member1, "댓글", false)
            );
            List<Reaction> reactions = List.of(new Reaction(member2, ReactionType.LIKE));

            Post post = Post.restore(
                    new PostId(10L), boardId, authorId, "제목", "내용", false, comments, reactions);

            assertThat(post.getId()).isEqualTo(new PostId(10L));
            assertThat(post.getComments()).hasSize(1);
            assertThat(post.likeCount()).isEqualTo(1);
        }
    }
}
