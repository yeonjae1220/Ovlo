package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.CreateCommentCommand;
import me.yeonjae.ovlo.application.dto.command.CreatePostCommand;
import me.yeonjae.ovlo.application.dto.command.DeletePostCommand;
import me.yeonjae.ovlo.application.dto.command.ReactToPostCommand;
import me.yeonjae.ovlo.application.dto.command.UnreactToPostCommand;
import me.yeonjae.ovlo.application.dto.result.CommentResult;
import me.yeonjae.ovlo.application.dto.result.PostResult;
import me.yeonjae.ovlo.application.dto.result.VerificationStatusResult;
import me.yeonjae.ovlo.application.port.in.verification.GetMyVerificationStatusQuery;
import me.yeonjae.ovlo.application.port.out.board.LoadBoardPort;
import me.yeonjae.ovlo.application.port.out.post.LoadPostPort;
import me.yeonjae.ovlo.application.port.out.post.SavePostPort;
import me.yeonjae.ovlo.domain.board.model.Board;
import me.yeonjae.ovlo.domain.board.model.BoardCategory;
import me.yeonjae.ovlo.domain.board.model.BoardId;
import me.yeonjae.ovlo.domain.board.model.LocationScope;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostCommandServiceTest {

    @Mock LoadPostPort loadPostPort;
    @Mock SavePostPort savePostPort;
    @Mock LoadBoardPort loadBoardPort;
    @Mock GetMyVerificationStatusQuery getMyVerificationStatusQuery;

    @InjectMocks
    PostCommandService service;

    private static final long UNIV_A = 100L;
    private static final long UNIV_B = 200L;

    /** UNIVERSITY 범위 게시판(트러스트 게이트 없음) — 소속 검사만 걸린다. */
    private static Board universityBoard(long universityId) {
        return Board.create("대학 게시판", "설명", BoardCategory.GENERAL,
                LocationScope.UNIVERSITY, new MemberId(99L), new UniversityId(universityId));
    }

    private static VerificationStatusResult.VerifiedUniversity verified(long universityId) {
        return new VerificationStatusResult.VerifiedUniversity(universityId, "s@univ.edu", Instant.now().toString());
    }

    private static VerificationStatusResult studentVerifiedAt(long universityId) {
        return new VerificationStatusResult("STUDENT", List.of(verified(universityId)));
    }

    private static Post savedPost(long postId, long authorId) {
        return Post.restore(new PostId(postId), new BoardId(1L), new MemberId(authorId),
                "제목", "내용", false, List.of(), List.of(), Instant.now());
    }

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

        private Board boardRequiring(String minTrustLevel) {
            Board board = Board.create("교환생 게시판", "설명", BoardCategory.GENERAL,
                    LocationScope.GLOBAL, new MemberId(99L), null);
            board.applyMinTrustLevel(minTrustLevel);
            return board;
        }

        @Test
        @DisplayName("게시판이 STUDENT 인증을 요구하는데 작성자가 미인증이면 FORBIDDEN")
        void blocksWhenTrustInsufficient() {
            given(loadBoardPort.findById(new BoardId(1L))).willReturn(Optional.of(boardRequiring("STUDENT")));
            given(getMyVerificationStatusQuery.getByMemberId(1L))
                    .willReturn(new VerificationStatusResult("UNVERIFIED", List.of()));

            assertThatThrownBy(() -> service.create(new CreatePostCommand(1L, 1L, "제목", "내용")))
                    .isInstanceOf(PostException.class)
                    .extracting("errorType").isEqualTo(PostException.ErrorType.FORBIDDEN);
            verify(savePostPort, org.mockito.Mockito.never()).save(any());
        }

        @Test
        @DisplayName("작성자 등급이 요구 등급 이상이면 작성 허용")
        void allowsWhenTrustSufficient() {
            given(loadBoardPort.findById(new BoardId(1L))).willReturn(Optional.of(boardRequiring("STUDENT")));
            given(getMyVerificationStatusQuery.getByMemberId(1L))
                    .willReturn(new VerificationStatusResult("EXCHANGE_VERIFIED", List.of()));
            Post saved = Post.restore(new PostId(11L), new BoardId(1L), new MemberId(1L),
                    "제목", "내용", false, List.of(), List.of(), Instant.now());
            given(savePostPort.save(any())).willReturn(saved);

            assertThat(service.create(new CreatePostCommand(1L, 1L, "제목", "내용")).id()).isEqualTo(11L);
        }

        @Test
        @DisplayName("게이트 없는 GLOBAL 게시판은 인증 현황을 조회하지 않는다")
        void skipsVerificationLookupWhenNoGate() {
            given(loadBoardPort.findById(new BoardId(1L))).willReturn(Optional.of(boardRequiring("UNVERIFIED")));
            given(savePostPort.save(any())).willReturn(savedPost(12L, 1L));

            service.create(new CreatePostCommand(1L, 1L, "제목", "내용"));

            verify(getMyVerificationStatusQuery, never()).getByMemberId(anyLong());
        }

        @Test
        @DisplayName("UNIVERSITY 게시판: 그 대학의 활성 인증 자격이 있으면 작성 허용")
        void allowsUniversityBoardWhenAuthorVerifiedAtThatUniversity() {
            given(loadBoardPort.findById(new BoardId(1L))).willReturn(Optional.of(universityBoard(UNIV_A)));
            given(getMyVerificationStatusQuery.getByMemberId(1L)).willReturn(studentVerifiedAt(UNIV_A));
            given(savePostPort.save(any())).willReturn(savedPost(13L, 1L));

            assertThat(service.create(new CreatePostCommand(1L, 1L, "제목", "내용")).id()).isEqualTo(13L);
        }

        @Test
        @DisplayName("UNIVERSITY 게시판: 인증 자격이 전혀 없으면 FORBIDDEN (트러스트 게이트가 없어도 소속 검사는 한다)")
        void blocksUniversityBoardWhenAuthorUnverified() {
            given(loadBoardPort.findById(new BoardId(1L))).willReturn(Optional.of(universityBoard(UNIV_A)));
            given(getMyVerificationStatusQuery.getByMemberId(1L))
                    .willReturn(new VerificationStatusResult("UNVERIFIED", List.of()));

            assertThatThrownBy(() -> service.create(new CreatePostCommand(1L, 1L, "제목", "내용")))
                    .isInstanceOf(PostException.class)
                    .extracting("errorType").isEqualTo(PostException.ErrorType.FORBIDDEN);
            verify(savePostPort, never()).save(any());
        }

        @Test
        @DisplayName("UNIVERSITY 게시판: 다른 대학 인증만 있으면 등급이 충분해도 FORBIDDEN")
        void blocksUniversityBoardWhenVerifiedAtOtherUniversityOnly() {
            given(loadBoardPort.findById(new BoardId(1L))).willReturn(Optional.of(universityBoard(UNIV_A)));
            given(getMyVerificationStatusQuery.getByMemberId(1L))
                    .willReturn(new VerificationStatusResult("EXCHANGE_VERIFIED", List.of(verified(UNIV_B))));

            assertThatThrownBy(() -> service.create(new CreatePostCommand(1L, 1L, "제목", "내용")))
                    .isInstanceOf(PostException.class)
                    .extracting("errorType").isEqualTo(PostException.ErrorType.FORBIDDEN);
            verify(savePostPort, never()).save(any());
        }

        @Test
        @DisplayName("UNIVERSITY 게시판 + 트러스트 게이트: 소속은 맞아도 등급이 모자라면 FORBIDDEN")
        void universityBoardStillEnforcesTrustGate() {
            Board board = universityBoard(UNIV_A);
            board.applyMinTrustLevel("EXCHANGE_VERIFIED");
            given(loadBoardPort.findById(new BoardId(1L))).willReturn(Optional.of(board));
            given(getMyVerificationStatusQuery.getByMemberId(1L)).willReturn(studentVerifiedAt(UNIV_A));

            assertThatThrownBy(() -> service.create(new CreatePostCommand(1L, 1L, "제목", "내용")))
                    .isInstanceOf(PostException.class)
                    .hasMessageContaining("EXCHANGE_VERIFIED")
                    .extracting("errorType").isEqualTo(PostException.ErrorType.FORBIDDEN);
            verify(savePostPort, never()).save(any());
        }

        @Test
        @DisplayName("UNIVERSITY 게시판인데 대학 ID가 비어 있으면(데이터 오류) 열지 않고 FORBIDDEN")
        void failsClosedWhenUniversityBoardHasNoUniversityId() {
            Board corrupted = Board.restore(new BoardId(1L), "대학 게시판", "설명", BoardCategory.GENERAL,
                    LocationScope.UNIVERSITY, new MemberId(99L), null, true);
            given(loadBoardPort.findById(new BoardId(1L))).willReturn(Optional.of(corrupted));
            given(getMyVerificationStatusQuery.getByMemberId(1L)).willReturn(studentVerifiedAt(UNIV_A));

            assertThatThrownBy(() -> service.create(new CreatePostCommand(1L, 1L, "제목", "내용")))
                    .isInstanceOf(PostException.class)
                    .extracting("errorType").isEqualTo(PostException.ErrorType.FORBIDDEN);
            verify(savePostPort, never()).save(any());
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

        @Test
        @DisplayName("UNIVERSITY 게시판 글에 그 대학 인증이 없는 회원이 댓글을 달면 FORBIDDEN")
        void blocksCommentOnUniversityBoardWhenNotMember() {
            PostId postId = new PostId(1L);
            Post post = Post.restore(postId, new BoardId(1L), new MemberId(9L), "제목", "내용",
                    false, new ArrayList<>(), new ArrayList<>(), Instant.now());
            given(loadPostPort.findById(postId)).willReturn(Optional.of(post));
            given(loadBoardPort.findById(new BoardId(1L))).willReturn(Optional.of(universityBoard(UNIV_A)));
            given(getMyVerificationStatusQuery.getByMemberId(2L))
                    .willReturn(new VerificationStatusResult("STUDENT", List.of(verified(UNIV_B))));

            assertThatThrownBy(() -> service.createComment(new CreateCommentCommand(1L, 2L, "댓글")))
                    .isInstanceOf(PostException.class)
                    .extracting("errorType").isEqualTo(PostException.ErrorType.FORBIDDEN);
            assertThat(post.getComments()).isEmpty();
            verify(savePostPort, never()).save(any());
        }

        @Test
        @DisplayName("UNIVERSITY 게시판 글에 그 대학 인증 회원은 댓글을 달 수 있다")
        void allowsCommentOnUniversityBoardWhenMember() {
            PostId postId = new PostId(1L);
            Post post = Post.restore(postId, new BoardId(1L), new MemberId(9L), "제목", "내용",
                    false, new ArrayList<>(), new ArrayList<>(), Instant.now());
            given(loadPostPort.findById(postId)).willReturn(Optional.of(post));
            given(loadBoardPort.findById(new BoardId(1L))).willReturn(Optional.of(universityBoard(UNIV_A)));
            given(getMyVerificationStatusQuery.getByMemberId(2L)).willReturn(studentVerifiedAt(UNIV_A));
            given(savePostPort.save(any())).willReturn(post);

            CommentResult result = service.createComment(new CreateCommentCommand(1L, 2L, "댓글"));

            assertThat(result.content()).isEqualTo("댓글");
            verify(savePostPort).save(post);
        }

        @Test
        @DisplayName("GLOBAL 게시판 글의 댓글은 인증 현황을 조회하지 않는다")
        void skipsVerificationLookupForNonUniversityBoard() {
            PostId postId = new PostId(1L);
            Post post = Post.restore(postId, new BoardId(1L), new MemberId(9L), "제목", "내용",
                    false, new ArrayList<>(), new ArrayList<>(), Instant.now());
            Board globalBoard = Board.create("자유", "설명", BoardCategory.GENERAL,
                    LocationScope.GLOBAL, new MemberId(99L), null);
            given(loadPostPort.findById(postId)).willReturn(Optional.of(post));
            given(loadBoardPort.findById(new BoardId(1L))).willReturn(Optional.of(globalBoard));
            given(savePostPort.save(any())).willReturn(post);

            service.createComment(new CreateCommentCommand(1L, 2L, "댓글"));

            verify(getMyVerificationStatusQuery, never()).getByMemberId(anyLong());
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

            service.react(new ReactToPostCommand(1L, 1L, "LIKE"));

            // 도메인 검증으로 in-memory 카운트는 오르지만, 영속화는 전체 저장이 아니라 회원 1행 upsert.
            assertThat(post.likeCount()).isEqualTo(1);
            verify(savePostPort).upsertReaction(postId, memberId, ReactionType.LIKE);
            verify(savePostPort, org.mockito.Mockito.never()).save(any());
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

            service.unreact(new UnreactToPostCommand(1L, 1L));

            assertThat(post.likeCount()).isEqualTo(0);
            verify(savePostPort).removeReaction(postId, memberId);
            verify(savePostPort, org.mockito.Mockito.never()).save(any());
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
