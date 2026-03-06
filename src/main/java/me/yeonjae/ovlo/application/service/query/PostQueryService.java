package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.PostPageResult;
import me.yeonjae.ovlo.application.dto.result.PostResult;
import me.yeonjae.ovlo.application.port.in.post.GetPostQuery;
import me.yeonjae.ovlo.application.port.out.post.LoadPostPort;
import me.yeonjae.ovlo.domain.board.model.BoardId;
import me.yeonjae.ovlo.domain.post.exception.PostException;
import me.yeonjae.ovlo.domain.post.model.PostId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PostQueryService implements GetPostQuery {

    private final LoadPostPort loadPostPort;

    public PostQueryService(LoadPostPort loadPostPort) {
        this.loadPostPort = loadPostPort;
    }

    @Override
    public PostResult getById(PostId postId) {
        return loadPostPort.findById(postId)
                .map(PostResult::from)
                .orElseThrow(() -> new PostException("게시글을 찾을 수 없습니다: " + postId.value()));
    }

    @Override
    public PostPageResult listByBoard(Long boardId, int page, int size) {
        BoardId id = new BoardId(boardId);
        int offset = page * size;
        List<PostResult> content = loadPostPort.findByBoardId(id, offset, size)
                .stream()
                .map(PostResult::from)
                .toList();
        long total = loadPostPort.countByBoardId(id);
        return new PostPageResult(content, total, page, size);
    }
}
