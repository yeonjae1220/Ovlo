package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.CreateCommentCommand;
import me.yeonjae.ovlo.application.dto.command.CreatePostCommand;
import me.yeonjae.ovlo.application.dto.command.DeleteCommentCommand;
import me.yeonjae.ovlo.application.dto.command.DeletePostCommand;
import me.yeonjae.ovlo.application.dto.command.ReactToPostCommand;
import me.yeonjae.ovlo.application.dto.command.UnreactToPostCommand;
import me.yeonjae.ovlo.application.dto.command.UpdatePostCommand;
import me.yeonjae.ovlo.application.dto.result.CommentResult;
import me.yeonjae.ovlo.application.dto.result.PostResult;
import me.yeonjae.ovlo.application.dto.result.VerificationStatusResult;
import me.yeonjae.ovlo.application.port.in.post.CreateCommentUseCase;
import me.yeonjae.ovlo.application.port.in.post.CreatePostUseCase;
import me.yeonjae.ovlo.application.port.in.post.DeleteCommentUseCase;
import me.yeonjae.ovlo.application.port.in.post.DeletePostUseCase;
import me.yeonjae.ovlo.application.port.in.post.ReactToPostUseCase;
import me.yeonjae.ovlo.application.port.in.post.UpdatePostUseCase;
import me.yeonjae.ovlo.application.port.in.verification.GetMyVerificationStatusQuery;
import me.yeonjae.ovlo.application.port.out.board.LoadBoardPort;
import me.yeonjae.ovlo.application.port.out.post.LoadPostPort;
import me.yeonjae.ovlo.application.port.out.post.SavePostPort;
import me.yeonjae.ovlo.domain.board.model.Board;
import me.yeonjae.ovlo.domain.board.model.BoardId;
import me.yeonjae.ovlo.domain.board.model.LocationScope;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.post.exception.PostException;
import me.yeonjae.ovlo.domain.verification.model.TrustLevel;
import me.yeonjae.ovlo.domain.post.model.CommentId;
import me.yeonjae.ovlo.domain.post.model.Post;
import me.yeonjae.ovlo.domain.post.model.PostId;
import me.yeonjae.ovlo.domain.post.model.ReactionType;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PostCommandService
        implements CreatePostUseCase, UpdatePostUseCase, CreateCommentUseCase,
                   DeleteCommentUseCase, ReactToPostUseCase, DeletePostUseCase {

    private final LoadPostPort loadPostPort;
    private final SavePostPort savePostPort;
    private final LoadBoardPort loadBoardPort;
    private final GetMyVerificationStatusQuery getMyVerificationStatusQuery;

    public PostCommandService(LoadPostPort loadPostPort, SavePostPort savePostPort,
                              LoadBoardPort loadBoardPort,
                              GetMyVerificationStatusQuery getMyVerificationStatusQuery) {
        this.loadPostPort = loadPostPort;
        this.savePostPort = savePostPort;
        this.loadBoardPort = loadBoardPort;
        this.getMyVerificationStatusQuery = getMyVerificationStatusQuery;
    }

    @Override
    public PostResult create(CreatePostCommand command) {
        BoardId boardId = new BoardId(command.boardId());
        loadBoardPort.findById(boardId).ifPresent(board -> enforcePostingGates(board, command.authorId()));
        Post post = Post.create(
                boardId,
                new MemberId(command.authorId()),
                command.title(),
                command.content()
        );
        return PostResult.from(savePostPort.save(post));
    }

    /**
     * 글쓰기 게이트. 게시판이 없으면 게이트 없음(존재 검증은 Post 생성·FK에 위임).
     * <ol>
     *   <li>트러스트 게이트 — 게시판 {@code minTrustLevel} 이상의 {@link TrustLevel}</li>
     *   <li>소속 게이트 — UNIVERSITY 범위면 그 대학의 활성 인증 자격 보유</li>
     * </ol>
     * 둘 다 필요 없으면 인증 현황 조회를 생략하고, 필요하면 한 번만 조회해 두 게이트가 공유한다.
     */
    private void enforcePostingGates(Board board, Long authorId) {
        TrustLevel required = TrustLevel.parseOrUnverified(board.getMinTrustLevel());
        boolean universityScoped = board.getScope() == LocationScope.UNIVERSITY;
        if (required == TrustLevel.UNVERIFIED && !universityScoped) {
            return;
        }
        VerificationStatusResult status = getMyVerificationStatusQuery.getByMemberId(authorId);
        requireTrust(required, status);
        if (universityScoped) {
            requireUniversityMembership(board, status);
        }
    }

    /** 댓글 게이트 — 소속 게이트만 적용한다(트러스트 게이트는 기존대로 글쓰기 전용). */
    private void enforceCommentGates(Board board, Long authorId) {
        if (board.getScope() != LocationScope.UNIVERSITY) {
            return;
        }
        requireUniversityMembership(board, getMyVerificationStatusQuery.getByMemberId(authorId));
    }

    private static void requireTrust(TrustLevel required, VerificationStatusResult status) {
        TrustLevel authorTrust = TrustLevel.parseOrUnverified(status.trustLevel());
        if (!authorTrust.atLeast(required)) {
            throw new PostException(
                    "이 게시판에 글을 쓰려면 " + required.name() + " 이상의 인증이 필요합니다",
                    PostException.ErrorType.FORBIDDEN);
        }
    }

    /**
     * UNIVERSITY 범위 게시판 소속 검사: 작성자가 게시판 대학의 <b>활성</b> 인증 자격
     * ({@code VerificationCredential.isActive()}, {@code verifiedUniversities}에 그것만 담긴다)을 가져야 한다.
     *
     * <p>회원 프로필의 {@code homeUniversityId}는 가입·온보딩에서 본인이 고른 자기 신고값이라
     * 소속 근거로 치지 않는다 — 인정하면 아무 대학이나 골라 그 게시판에 쓸 수 있어 이 게이트가 무의미해진다
     * (docs/adr/0005). 게시판 대학이 비어 있으면(데이터 오류) 열지 않고 닫는다.
     */
    private static void requireUniversityMembership(Board board, VerificationStatusResult status) {
        UniversityId boardUniversity = board.getUniversityId();
        boolean member = boardUniversity != null && status.verifiedUniversities().stream()
                .anyMatch(v -> boardUniversity.value().equals(v.universityId()));
        if (!member) {
            throw new PostException(
                    "이 게시판은 해당 대학 인증을 마친 회원만 글과 댓글을 쓸 수 있습니다",
                    PostException.ErrorType.FORBIDDEN);
        }
    }

    @Override
    public PostResult update(UpdatePostCommand command) {
        Post post = loadPostOrThrow(command.postId());
        if (!post.getAuthorId().equals(new MemberId(command.requesterId()))) {
            throw new PostException("게시글을 수정할 권한이 없습니다", PostException.ErrorType.FORBIDDEN);
        }
        post.update(command.title(), command.content());
        return PostResult.from(savePostPort.save(post));
    }

    @Override
    public CommentResult createComment(CreateCommentCommand command) {
        Post post = loadPostOrThrow(command.postId());
        loadBoardPort.findById(post.getBoardId())
                .ifPresent(board -> enforceCommentGates(board, command.authorId()));
        var comment = post.addComment(new MemberId(command.authorId()), command.content());
        savePostPort.save(post);
        return CommentResult.from(comment);
    }

    @Override
    public void deleteComment(DeleteCommentCommand command) {
        Post post = loadPostOrThrow(command.postId());
        post.deleteComment(new CommentId(command.commentId()), new MemberId(command.requesterId()));
        savePostPort.save(post);
    }

    @Override
    public void react(ReactToPostCommand command) {
        Post post = loadPostOrThrow(command.postId());
        ReactionType type = ReactionType.valueOf(command.reactionType());
        MemberId memberId = new MemberId(command.memberId());
        // 도메인 불변식 검증(중복 CONFLICT · 좋아요↔싫어요 전환 · 삭제 게시글 차단).
        post.react(memberId, type);
        // A안: 전체 애그리거트 저장 대신 해당 회원 1행만 upsert(+ 카운트 원자 증감).
        // 서로 다른 회원의 동시 반응은 서로 다른 행을 건드리므로 낙관적 락/재시도가 필요 없다.
        savePostPort.upsertReaction(post.getId(), memberId, type);
    }

    @Override
    public void unreact(UnreactToPostCommand command) {
        Post post = loadPostOrThrow(command.postId());
        MemberId memberId = new MemberId(command.memberId());
        post.unreact(memberId); // 미반응 상태면 NOT_FOUND
        savePostPort.removeReaction(post.getId(), memberId);
    }

    @Override
    public void delete(DeletePostCommand command) {
        Post post = loadPostOrThrow(command.postId());
        if (!post.getAuthorId().equals(new MemberId(command.requesterId()))) {
            throw new PostException("게시글을 삭제할 권한이 없습니다", PostException.ErrorType.FORBIDDEN);
        }
        post.delete();
        savePostPort.save(post);
    }

    private Post loadPostOrThrow(Long postId) {
        return loadPostPort.findById(new PostId(postId))
                .orElseThrow(() -> new PostException("게시글을 찾을 수 없습니다: " + postId, PostException.ErrorType.NOT_FOUND));
    }
}
