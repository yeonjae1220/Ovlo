package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.CommentResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.PostResult;
import me.yeonjae.ovlo.application.port.in.post.GetPostQuery;
import me.yeonjae.ovlo.application.port.out.board.LoadBoardPort;
import me.yeonjae.ovlo.application.port.out.moderation.MemberBlockPort;
import me.yeonjae.ovlo.application.port.out.post.LoadPostPort;
import me.yeonjae.ovlo.domain.board.model.BoardId;
import me.yeonjae.ovlo.domain.post.exception.PostException;
import me.yeonjae.ovlo.domain.post.model.Post;
import me.yeonjae.ovlo.domain.post.model.PostId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PostQueryService implements GetPostQuery {

    private final LoadPostPort loadPostPort;
    private final LoadBoardPort loadBoardPort;
    private final MemberBlockPort memberBlockPort;

    public PostQueryService(LoadPostPort loadPostPort, LoadBoardPort loadBoardPort, MemberBlockPort memberBlockPort) {
        this.loadPostPort = loadPostPort;
        this.loadBoardPort = loadBoardPort;
        this.memberBlockPort = memberBlockPort;
    }

    /** 조회자와 차단 관계인 회원. 조회자가 없으면 거르지 않는다. */
    private Set<Long> hiddenAuthorsFor(Long viewerId) {
        return viewerId == null ? Set.of() : memberBlockPort.relatedMemberIds(viewerId);
    }

    private static PostException notFound(PostId postId) {
        return new PostException("게시글을 찾을 수 없습니다: " + postId.value(), PostException.ErrorType.NOT_FOUND);
    }

    @Override
    public PostResult getById(PostId postId) {
        return getById(postId, null);
    }

    @Override
    public PostResult getById(PostId postId, Long viewerId) {
        Set<Long> hidden = hiddenAuthorsFor(viewerId);
        Post post = loadPostPort.findById(postId)
                .filter(p -> !hidden.contains(p.getAuthorId().value()))
                .orElseThrow(() -> notFound(postId));
        return PostResult.from(post, viewerId).withoutCommentsBy(hidden);
    }

    @Override
    public PageResult<PostResult> listByBoard(Long boardId, Long viewerId, int page, int size) {
        BoardId id = new BoardId(boardId);
        Set<Long> hidden = hiddenAuthorsFor(viewerId);
        List<Post> posts = loadPostPort.findByBoardId(id, hidden, page * size, size);
        var counts = loadPostPort.countComments(postIds(posts), hidden);
        List<PostResult> content = posts.stream()
                .map(p -> PostResult.fromSummary(p).withCommentCount(counts.getOrDefault(p.getId().value(), 0L)))
                .toList();
        return PageResult.of(content, loadPostPort.countByBoardId(id, hidden), page, size);
    }

    @Override
    public PageResult<PostResult> listAll(Long viewerId, int page, int size) {
        Set<Long> hidden = hiddenAuthorsFor(viewerId);
        List<Post> posts = loadPostPort.findAll(hidden, page * size, size);
        return PageResult.of(withBoardNames(posts, hidden), loadPostPort.count(hidden), page, size);
    }

    @Override
    public PageResult<PostResult> listByAuthor(Long authorId, Long viewerId, int page, int size) {
        Set<Long> hidden = hiddenAuthorsFor(viewerId);
        if (hidden.contains(authorId)) {
            return PageResult.of(List.of(), 0, page, size);
        }
        List<Post> posts = loadPostPort.findByAuthorId(authorId, page * size, size);
        return PageResult.of(withBoardNames(posts, hidden), loadPostPort.countByAuthorId(authorId), page, size);
    }

    private List<PostResult> withBoardNames(List<Post> posts, Set<Long> hidden) {
        // 페이지 내 distinct boardIds를 배치 조회하여 N+1 방지
        Map<Long, String> boardNames = loadBoardPort.findByIds(posts.stream()
                .map(p -> p.getBoardId().value()).distinct().toList()).stream()
                .collect(Collectors.toMap(b -> b.getId().value(), b -> b.getName()));
        var counts = loadPostPort.countComments(postIds(posts), hidden);
        return posts.stream()
                .map(p -> PostResult.fromSummary(p, boardNames.getOrDefault(p.getBoardId().value(), ""))
                        .withCommentCount(counts.getOrDefault(p.getId().value(), 0L)))
                .toList();
    }

    private static List<Long> postIds(List<Post> posts) {
        return posts.stream().map(p -> p.getId().value()).toList();
    }

    @Override
    public PageResult<CommentResult> getComments(PostId postId, Long viewerId, int page, int size) {
        Set<Long> hidden = hiddenAuthorsFor(viewerId);
        boolean visible = loadPostPort.findById(postId)
                .map(p -> !p.isDeleted() && !hidden.contains(p.getAuthorId().value()))
                .orElse(false);
        if (!visible) {
            throw notFound(postId);
        }
        List<CommentResult> content = loadPostPort.findCommentsByPostId(postId, hidden, page * size, size)
                .stream()
                .map(CommentResult::from)
                .toList();
        return PageResult.of(content, loadPostPort.countCommentsByPostId(postId, hidden), page, size);
    }
}
