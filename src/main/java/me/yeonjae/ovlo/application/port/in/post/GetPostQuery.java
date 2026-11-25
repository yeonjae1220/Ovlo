package me.yeonjae.ovlo.application.port.in.post;

import me.yeonjae.ovlo.application.dto.result.CommentResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.PostResult;
import me.yeonjae.ovlo.domain.post.model.PostId;

/**
 * viewerId 는 조회하는 회원이다. 그 회원과 차단 관계(어느 쪽이든)인 작성자의 게시글·댓글은 보이지 않는다.
 * null 이면(비로그인·관리자) 거르지 않는다.
 */
public interface GetPostQuery {
    PostResult getById(PostId postId);
    PostResult getById(PostId postId, Long viewerId);
    PageResult<PostResult> listByBoard(Long boardId, Long viewerId, int page, int size);
    PageResult<PostResult> listAll(Long viewerId, int page, int size);
    PageResult<PostResult> listByAuthor(Long authorId, Long viewerId, int page, int size);
    PageResult<CommentResult> getComments(PostId postId, Long viewerId, int page, int size);
}
