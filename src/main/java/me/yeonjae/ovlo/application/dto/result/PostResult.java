package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.post.model.Post;
import me.yeonjae.ovlo.domain.post.model.ReactionType;

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
        List<CommentResult> comments,
        boolean likedByMe
) {
    public static PostResult from(Post post, Long requesterId) {
        List<CommentResult> commentResults = post.getComments().stream()
                .map(CommentResult::from)
                .toList();

        boolean likedByMe = requesterId != null && post.getReactions().stream()
                .anyMatch(r -> r.memberId().value().equals(requesterId) && r.type() == ReactionType.LIKE);

        return new PostResult(
                post.getId() != null ? post.getId().value() : null,
                post.getBoardId().value(),
                post.getAuthorId().value(),
                post.getTitle(),
                post.getContent(),
                post.isDeleted(),
                post.likeCount(),
                post.dislikeCount(),
                commentResults,
                likedByMe
        );
    }

    public static PostResult from(Post post) {
        return from(post, null);
    }
}
