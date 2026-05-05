package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.CreateCommentCommand;
import me.yeonjae.ovlo.application.dto.command.CreatePostCommand;
import me.yeonjae.ovlo.application.dto.command.DeletePostCommand;
import me.yeonjae.ovlo.application.dto.command.ReactToPostCommand;
import me.yeonjae.ovlo.application.dto.command.UnreactToPostCommand;
import me.yeonjae.ovlo.application.dto.result.CommentResult;
import me.yeonjae.ovlo.application.dto.result.PostResult;
import me.yeonjae.ovlo.application.port.out.post.LoadPostPort;
import me.yeonjae.ovlo.application.port.out.post.SavePostPort;
import me.yeonjae.ovlo.domain.board.model.BoardId;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.post.exception.PostException;
import me.yeonjae.ovlo.domain.post.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostCommandServiceTest {

    @Mock LoadPostPort loadPostPort;
    @Mock SavePostPort savePostPort;

    @InjectMocks
    PostCommandService service;

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("게시글을 생성하면 저장된 결과를 반환한다")
        void shouldCreate_post() {
            CreatePostCommand command = new CreatePostCommand(1L, 1L, "제목", "내용");

            Post saved = Post.restore(new PostId(10L), new BoardId(1L), new MemberId(1L),
                    "제목", "내용", false, List.of(), List.of(), Instant.now());
            given(savePostPort.save(any())).willReturn(saved);

            PostResult result = service.create(command);

            assertThat(result.id()).isEqualTo(10L);
            assertThat(result.title()).isEqualTo("제목");
            assertThat(result.deleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("createComment()")
    class CreateComment {

        @Test
        @DisplayName("존재하는 게시글에 댓글을 달 수 있다")
        void shouldCreateComment() {
            PostId postId = new PostId(1L);
            MemberId memberId = new MemberId(2L);
            Post post = Post.restore(postId, new BoardId(1L), memberId, "제목", "내용",
                    false, new ArrayList<>(), new ArrayList<>(), Instant.now());

            given(loadPostPort.findById(postId)).willReturn(Optional.of(post));
            given(savePostPort.save(any())).willReturn(post);

            CommentResult result = service.createComment(new CreateCommentCommand(1L, 2L, "댓글 내용"));

            assertThat(result.content()).isEqualTo("댓글 내용");
            verify(savePostPort).save(post);
        }

        @Test
        @DisplayName("존재하지 않는 게시글에 댓글 작성 시 예외가 발생한다")
        void shouldThrow_whenPostNotFound() {
            given(loadPostPort.findById(any())).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.createComment(new CreateCommentCommand(999L, 1L, "댓글")))
                    .isInstanceOf(PostException.class)
                    .hasMessageContaining("게시글을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("react()")
    class React {

        @Test
        @DisplayName("게시글에 좋아요 반응을 추가할 수 있다")
        void shouldReact_withLike() {
            PostId postId = new PostId(1L);
            MemberId memberId = new MemberId(1L);
            Post post = Post.restore(postId, new BoardId(1L), memberId, "제목", "내용",
                    false, new ArrayList<>(), new ArrayList<>(), Instant.now());

            given(loadPostPort.findById(postId)).willReturn(Optional.of(post));
            given(savePostPort.save(any())).willReturn(post);

            service.react(new ReactToPostCommand(1L, 1L, "LIKE"));

            assertThat(post.likeCount()).isEqualTo(1);
            verify(savePostPort).save(post);
        }

        @Test
        @DisplayName("존재하지 않는 게시글에 반응 시 예외가 발생한다")
        void shouldThrow_whenPostNotFound() {
            given(loadPostPort.findById(any())).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.react(new ReactToPostCommand(999L, 1L, "LIKE")))
                    .isInstanceOf(PostException.class)
                    .hasMessageContaining("게시글을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("unreact()")
    class Unreact {

        @Test
        @DisplayName("좋아요 반응을 취소할 수 있다")
        void shouldUnreact() {
            PostId postId = new PostId(1L);
            MemberId memberId = new MemberId(1L);
            List<Reaction> reactions = new ArrayList<>();
            reactions.add(new Reaction(memberId, ReactionType.LIKE));
            Post post = Post.restore(postId, new BoardId(1L), memberId, "제목", "내용",
                    false, new ArrayList<>(), reactions, Instant.now());

            given(loadPostPort.findById(postId)).willReturn(Optional.of(post));
            given(savePostPort.save(any())).willReturn(post);

            service.unreact(new UnreactToPostCommand(1L, 1L));

            assertThat(post.likeCount()).isEqualTo(0);
            verify(savePostPort).save(post);
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("작성자가 게시글을 삭제할 수 있다")
        void shouldDelete_byAuthor() {
            PostId postId = new PostId(1L);
            MemberId authorId = new MemberId(1L);
            Post post = Post.restore(postId, new BoardId(1L), authorId, "제목", "내용",
                    false, new ArrayList<>(), new ArrayList<>(), Instant.now());

            given(loadPostPort.findById(postId)).willReturn(Optional.of(post));
            given(savePostPort.save(any())).willReturn(post);

            service.delete(new DeletePostCommand(1L, 1L));

            assertThat(post.isDeleted()).isTrue();
            verify(savePostPort).save(post);
        }

        @Test
        @DisplayName("작성자가 아닌 회원이 삭제 시 예외가 발생한다")
        void shouldThrow_whenNotAuthor() {
            PostId postId = new PostId(1L);
            MemberId authorId = new MemberId(1L);
            Post post = Post.restore(postId, new BoardId(1L), authorId, "제목", "내용",
                    false, new ArrayList<>(), new ArrayList<>(), Instant.now());

            given(loadPostPort.findById(postId)).willReturn(Optional.of(post));

            assertThatThrownBy(() -> service.delete(new DeletePostCommand(1L, 99L)))
                    .isInstanceOf(PostException.class)
                    .hasMessageContaining("게시글을 삭제할 권한이 없습니다");
        }

        @Test
        @DisplayName("존재하지 않는 게시글 삭제 시 예외가 발생한다")
        void shouldThrow_whenPostNotFound() {
            given(loadPostPort.findById(any())).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(new DeletePostCommand(999L, 1L)))
                    .isInstanceOf(PostException.class)
                    .hasMessageContaining("게시글을 찾을 수 없습니다");
        }
    }
}
