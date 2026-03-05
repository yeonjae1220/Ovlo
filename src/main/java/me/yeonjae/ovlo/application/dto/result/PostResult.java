package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.post.model.Post;

import java.util.List;

public record PostResult(
        Long id,
        Long boardId,
        Long authorId,
        String title,
        String content,
        boolean deleted,
        long likeCount,
        long dislikeCount,
        List<CommentResult> comments
) {
    public static PostResult from(Post post) {
        List<CommentResult> commentResults = post.getComments().stream()
                .map(CommentResult::from)
                .toList();

        return new PostResult(
                post.getId() != null ? post.getId().value() : null,
                post.getBoardId().value(),
                post.getAuthorId().value(),
                post.getTitle(),
                post.getContent(),
                post.isDeleted(),
                post.likeCount(),
                post.dislikeCount(),
                commentResults
        );
    }
}
