package me.yeonjae.ovlo.domain.post.model;

import me.yeonjae.ovlo.domain.member.model.MemberId;

import java.util.Objects;

public class Comment {

    private CommentId id;
    private PostId postId;
    private MemberId authorId;
    private String content;
    private boolean deleted;

    private Comment() {}

    public static Comment create(MemberId authorId, String content) {
        Objects.requireNonNull(authorId, "댓글 작성자 ID는 필수입니다");
        Objects.requireNonNull(content, "댓글 내용은 필수입니다");
        if (content.isBlank()) throw new IllegalArgumentException("댓글 내용은 빈 값일 수 없습니다");

        Comment comment = new Comment();
        comment.authorId = authorId;
        comment.content = content;
        comment.deleted = false;
        return comment;
    }

    /** persistence 계층 전용: DB에서 모든 필드를 복원할 때 사용 */
    public static Comment restore(
            CommentId id,
            PostId postId,
            MemberId authorId,
            String content,
            boolean deleted) {

        Comment comment = new Comment();
        comment.id = id;
        comment.postId = postId;
        comment.authorId = authorId;
        comment.content = content;
        comment.deleted = deleted;
        return comment;
    }

    public void delete() {
        if (deleted) {
            throw new IllegalStateException("이미 삭제된 댓글입니다");
        }
        this.deleted = true;
    }

    public CommentId getId() { return id; }
    public PostId getPostId() { return postId; }
    public MemberId getAuthorId() { return authorId; }
    public String getContent() { return content; }
    public boolean isDeleted() { return deleted; }
}
