package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.PostResult;
import me.yeonjae.ovlo.application.port.in.post.GetPostQuery;
import me.yeonjae.ovlo.application.port.out.post.LoadPostPort;
import me.yeonjae.ovlo.domain.post.exception.PostException;
import me.yeonjae.ovlo.domain.post.model.PostId;
import org.springframework.stereotype.Service;

@Service
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
}
