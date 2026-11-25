package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.post.model.Post;
import me.yeonjae.ovlo.domain.post.model.ReactionType;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record PostResult(
        Long id,
        Long boardId,
        String boardName,
        Long authorId,
        String title,
        String content,
        boolean deleted,
        long likeCount,
        long dislikeCount,
        List<CommentResult> comments,
        Instant createdAt,
        boolean likedByMe,
        long commentCount
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
                null,
                post.getAuthorId().value(),
                post.getTitle(),
                post.getContent(),
                post.isDeleted(),
                post.likeCount(),
                post.dislikeCount(),
                commentResults,
                post.getCreatedAt(),
                likedByMe,
                commentResults.stream().filter(c -> !c.deleted()).count()
        );
    }

    public static PostResult from(Post post) {
        return from(post, null);
    }

    public static PostResult fromSummary(Post post) {
        return new PostResult(
                post.getId() != null ? post.getId().value() : null,
                post.getBoardId().value(),
                null,
                post.getAuthorId().value(),
                post.getTitle(),
                post.getContent(),
                post.isDeleted(),
                post.likeCount(),
                post.dislikeCount(),
                List.of(),
                post.getCreatedAt(),
                false, 0
        );
    }

    public static PostResult fromSummary(Post post, String boardName) {
        return new PostResult(
                post.getId() != null ? post.getId().value() : null,
                post.getBoardId().value(),
                boardName,
                post.getAuthorId().value(),
                post.getTitle(),
                post.getContent(),
                post.isDeleted(),
                post.likeCount(),
                post.dislikeCount(),
                List.of(),
                post.getCreatedAt(),
                false, 0
        );
    }

    /** 조회자와 차단 관계인 회원의 댓글을 뺀다. 댓글 수도 다시 센다. */
    public PostResult withoutCommentsBy(Set<Long> hiddenAuthorIds) {
        if (hiddenAuthorIds.isEmpty() || comments.isEmpty()) return this;
        List<CommentResult> visible = comments.stream()
                .filter(c -> !hiddenAuthorIds.contains(c.authorId()))
                .toList();
        return new PostResult(id, boardId, boardName, authorId, title, content, deleted,
                likeCount, dislikeCount, visible, createdAt, likedByMe,
                visible.stream().filter(c -> !c.deleted()).count());
    }

    public PostResult withCommentCount(long count) {
        return new PostResult(id, boardId, boardName, authorId, title, content, deleted,
                likeCount, dislikeCount, comments, createdAt, likedByMe, count);
    }
}
