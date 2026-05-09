package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.CommentResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.PostResult;
import me.yeonjae.ovlo.application.port.out.post.LoadPostPort;
import me.yeonjae.ovlo.domain.board.model.BoardId;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.post.exception.PostException;
import me.yeonjae.ovlo.domain.post.model.Comment;
import me.yeonjae.ovlo.domain.post.model.CommentId;
import me.yeonjae.ovlo.domain.post.model.Post;
import me.yeonjae.ovlo.domain.post.model.PostId;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostQueryServiceTest {

    @Mock
    private LoadPostPort loadPostPort;

    @InjectMocks
    private PostQueryService service;

    private Post activePost;

    @BeforeEach
    void setUp() {
        activePost = Post.restore(new PostId(1L), new BoardId(1L), new MemberId(1L),
                "제목", "내용", false, List.of(), List.of(), Instant.now());
    }

    @Nested
    @DisplayName("게시글 단건 조회")
    class GetById {

        @Test
        @DisplayName("ID로 게시글을 조회할 수 있다")
        void shouldGetById() {
            given(loadPostPort.findById(new PostId(1L))).willReturn(Optional.of(activePost));

            PostResult result = service.getById(new PostId(1L));

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.title()).isEqualTo("제목");
            assertThat(result.likeCount()).isEqualTo(0);
            assertThat(result.comments()).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 예외가 발생한다")
        void shouldThrow_whenNotFound() {
            given(loadPostPort.findById(any())).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(new PostId(999L)))
                    .isInstanceOf(PostException.class)
                    .hasMessageContaining("게시글을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("게시판 게시글 목록 조회")
    class ListByBoard {

        @Test
        @DisplayName("게시판 ID로 게시글 목록을 페이지네이션으로 조회한다")
        void shouldListByBoard_withPagination() {
            given(loadPostPort.findByBoardId(new BoardId(1L), 0, 20))
                    .willReturn(List.of(activePost));
            given(loadPostPort.countByBoardId(new BoardId(1L))).willReturn(1L);

            PageResult<PostResult> result = service.listByBoard(1L, 0, 20);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).id()).isEqualTo(1L);
            assertThat(result.totalElements()).isEqualTo(1L);
        }

        @Test
        @DisplayName("페이지 offset이 올바르게 계산된다")
        void shouldCalculateOffset_correctly() {
            given(loadPostPort.findByBoardId(eq(new BoardId(1L)), eq(40), eq(20)))
                    .willReturn(List.of());
            given(loadPostPort.countByBoardId(new BoardId(1L))).willReturn(0L);

            service.listByBoard(1L, 2, 20);

            verify(loadPostPort).findByBoardId(new BoardId(1L), 40, 20);
        }

        @Test
        @DisplayName("게시글이 없으면 빈 목록을 반환한다")
        void shouldReturnEmpty_whenNoPosts() {
            given(loadPostPort.findByBoardId(any(), anyInt(), anyInt())).willReturn(List.of());
            given(loadPostPort.countByBoardId(any())).willReturn(0L);

            PageResult<PostResult> result = service.listByBoard(1L, 0, 20);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
            assertThat(result.hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("댓글 목록 조회")
    class GetComments {

        @Test
        @DisplayName("존재하는 게시글의 댓글 목록을 페이지네이션으로 조회한다")
        void shouldGetComments_withPagination() {
            PostId postId = new PostId(1L);
            Comment comment = Comment.restore(
                    new CommentId(10L), postId, new MemberId(2L), "댓글 내용", false);

            given(loadPostPort.findById(postId)).willReturn(Optional.of(activePost));
            given(loadPostPort.findCommentsByPostId(postId, 0, 10)).willReturn(List.of(comment));
            given(loadPostPort.countCommentsByPostId(postId)).willReturn(1L);

            PageResult<CommentResult> result = service.getComments(postId, 0, 10);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).content()).isEqualTo("댓글 내용");
            assertThat(result.content().get(0).authorId()).isEqualTo(2L);
            assertThat(result.totalElements()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 게시글의 댓글 조회 시 예외가 발생한다")
        void shouldThrow_whenPostNotFound() {
            PostId postId = new PostId(999L);
            given(loadPostPort.findById(postId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getComments(postId, 0, 10))
                    .isInstanceOf(PostException.class)
                    .hasMessageContaining("게시글을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("삭제된 게시글의 댓글 조회 시 예외가 발생한다")
        void shouldThrow_whenPostDeleted() {
            PostId postId = new PostId(1L);
            Post deletedPost = Post.restore(postId, new BoardId(1L), new MemberId(1L),
                    "제목", "내용", true, List.of(), List.of(), Instant.now());
            given(loadPostPort.findById(postId)).willReturn(Optional.of(deletedPost));

            assertThatThrownBy(() -> service.getComments(postId, 0, 10))
                    .isInstanceOf(PostException.class)
                    .hasMessageContaining("게시글을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("댓글이 없으면 빈 목록을 반환한다")
        void shouldReturnEmpty_whenNoComments() {
            PostId postId = new PostId(1L);
            given(loadPostPort.findById(postId)).willReturn(Optional.of(activePost));
            given(loadPostPort.findCommentsByPostId(postId, 0, 10)).willReturn(List.of());
            given(loadPostPort.countCommentsByPostId(postId)).willReturn(0L);

            PageResult<CommentResult> result = service.getComments(postId, 0, 10);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }

        @Test
        @DisplayName("페이지 offset이 올바르게 계산된다")
        void shouldCalculateOffset_correctly() {
            PostId postId = new PostId(1L);
            given(loadPostPort.findById(postId)).willReturn(Optional.of(activePost));
            given(loadPostPort.findCommentsByPostId(postId, 20, 10)).willReturn(List.of());
            given(loadPostPort.countCommentsByPostId(postId)).willReturn(0L);

            service.getComments(postId, 2, 10);

            verify(loadPostPort).findCommentsByPostId(postId, 20, 10);
        }
    }
}
