package me.yeonjae.ovlo.application.port.out.post;

import me.yeonjae.ovlo.domain.post.model.Comment;
import me.yeonjae.ovlo.domain.post.model.PostId;

public interface SaveCommentPort {
    Comment insertComment(PostId postId, Comment comment);
    void deleteComment(PostId postId, long commentId, long authorId);
}
