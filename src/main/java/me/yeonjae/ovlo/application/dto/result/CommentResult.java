package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.post.model.Comment;

public record CommentResult(
        Long id,
        Long postId,
        Long authorId,
        String content,
        boolean deleted
) {
    public static CommentResult from(Comment comment) {
        return new CommentResult(
                comment.getId() != null ? comment.getId().value() : null,
                comment.getPostId() != null ? comment.getPostId().value() : null,
                comment.getAuthorId().value(),
                comment.getContent(),
                comment.isDeleted()
        );
    }
}
