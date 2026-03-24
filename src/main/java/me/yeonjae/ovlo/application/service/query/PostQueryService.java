package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.CommentResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.PostResult;
import me.yeonjae.ovlo.application.port.in.post.GetPostQuery;
import me.yeonjae.ovlo.application.port.out.post.LoadPostPort;
import me.yeonjae.ovlo.domain.board.model.BoardId;
import me.yeonjae.ovlo.domain.post.exception.PostException;
import me.yeonjae.ovlo.domain.post.model.PostId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PostQueryService implements GetPostQuery {

    private final LoadPostPort loadPostPort;

    public PostQueryService(LoadPostPort loadPostPort) {
        this.loadPostPort = loadPostPort;
    }

    @Override
    public PostResult getById(PostId postId) {
        return getById(postId, null);
    }

    @Override
    public PostResult getById(PostId postId, Long requesterId) {
        return loadPostPort.findById(postId)
                .map(post -> PostResult.from(post, requesterId))
                .orElseThrow(() -> new PostException("게시글을 찾을 수 없습니다: " + postId.value(), PostException.ErrorType.NOT_FOUND));
    }

    @Override
    public PageResult<PostResult> listByBoard(Long boardId, int page, int size) {
        BoardId id = new BoardId(boardId);
        int offset = page * size;
        List<PostResult> content = loadPostPort.findByBoardId(id, offset, size)
                .stream()
                .map(PostResult::fromSummary)
                .toList();
        long total = loadPostPort.countByBoardId(id);
        return PageResult.of(content, total, page, size);
    }

    @Override
    public PageResult<CommentResult> getComments(PostId postId, int page, int size) {
        if (!loadPostPort.findById(postId).map(p -> !p.isDeleted()).orElse(false)) {
            throw new PostException("게시글을 찾을 수 없습니다: " + postId.value(), PostException.ErrorType.NOT_FOUND);
        }
        int offset = page * size;
        List<CommentResult> content = loadPostPort.findCommentsByPostId(postId, offset, size)
                .stream()
                .map(CommentResult::from)
                .collect(Collectors.toList());
        long total = loadPostPort.countCommentsByPostId(postId);
        return PageResult.of(content, total, page, size);
    }
}
