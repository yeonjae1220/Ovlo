package me.yeonjae.ovlo.application.port.in.post;

import me.yeonjae.ovlo.application.dto.result.PostResult;
import me.yeonjae.ovlo.domain.post.model.PostId;

public interface GetPostQuery {
    PostResult getById(PostId postId);
}
