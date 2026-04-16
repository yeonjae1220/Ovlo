package me.yeonjae.ovlo.adapter.in.web.dto.response;

import me.yeonjae.ovlo.domain.post.model.Post;

public record AdminPostResponse(
        Long id,
        Long boardId,
        Long authorId,
        String title,
        long likeCount,
        long dislikeCount,
        boolean deleted
) {
    public static AdminPostResponse of(Post post) {
        return new AdminPostResponse(
                post.getId() != null ? post.getId().value() : null,
                post.getBoardId().value(),
                post.getAuthorId().value(),
                post.getTitle(),
                post.likeCount(),
                post.dislikeCount(),
                post.isDeleted()
        );
    }
}
