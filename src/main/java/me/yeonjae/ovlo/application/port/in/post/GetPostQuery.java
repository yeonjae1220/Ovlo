package me.yeonjae.ovlo.application.port.in.post;

import me.yeonjae.ovlo.application.dto.result.CommentResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.PostResult;
import me.yeonjae.ovlo.domain.post.model.PostId;

public interface GetPostQuery {
    PostResult getById(PostId postId);
    PostResult getById(PostId postId, Long requesterId);
    PageResult<PostResult> listByBoard(Long boardId, int page, int size);
    PageResult<CommentResult> getComments(PostId postId, int page, int size);
}
