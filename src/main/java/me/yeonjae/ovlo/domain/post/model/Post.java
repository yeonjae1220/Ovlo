package me.yeonjae.ovlo.domain.post.model;

import me.yeonjae.ovlo.domain.board.model.BoardId;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.post.exception.PostException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Post {

    private PostId id;
    private BoardId boardId;
    private MemberId authorId;
    private String title;
    private String content;
    private boolean deleted;
    private final List<Comment> comments;
    private final List<Reaction> reactions;

    private Post() {
        this.comments = new ArrayList<>();
        this.reactions = new ArrayList<>();
    }

    public static Post create(
            BoardId boardId,
            MemberId authorId,
            String title,
            String content) {

        Objects.requireNonNull(boardId, "게시판 ID는 필수입니다");
        Objects.requireNonNull(authorId, "게시글 작성자 ID는 필수입니다");
        validateTitle(title);
        validateContent(content);

        Post post = new Post();
        post.boardId = boardId;
        post.authorId = authorId;
        post.title = title;
        post.content = content;
        post.deleted = false;
        return post;
    }

    /** persistence 계층 전용: DB에서 모든 필드를 복원할 때 사용 */
    public static Post restore(
            PostId id,
            BoardId boardId,
            MemberId authorId,
            String title,
            String content,
            boolean deleted,
            List<Comment> comments,
            List<Reaction> reactions) {

        Post post = new Post();
        post.id = id;
        post.boardId = boardId;
        post.authorId = authorId;
        post.title = title;
        post.content = content;
        post.deleted = deleted;
        post.comments.addAll(comments);
        post.reactions.addAll(reactions);
        return post;
    }

    public Comment addComment(MemberId authorId, String content) {
        if (deleted) {
            throw new PostException("삭제된 게시글에는 댓글을 달 수 없습니다");
        }
        Comment comment = Comment.create(authorId, content);
        comments.add(comment);
        return comment;
    }

    /**
     * 게시글에 반응(좋아요/싫어요)을 추가한다.
     * - 이미 같은 반응: 예외
     * - 반대 반응: 기존 반응 제거 후 새 반응 추가
     * - 반응 없음: 새 반응 추가
     */
    public void react(MemberId memberId, ReactionType type) {
        if (deleted) {
            throw new PostException("삭제된 게시글에는 반응할 수 없습니다");
        }

        boolean alreadyReacted = reactions.stream()
                .anyMatch(r -> r.memberId().equals(memberId) && r.type() == type);
        if (alreadyReacted) {
            throw new PostException("이미 동일한 반응을 했습니다");
        }

        // 반대 반응이 있으면 제거 후 새 반응 추가
        reactions.removeIf(r -> r.memberId().equals(memberId));
        reactions.add(new Reaction(memberId, type));
    }

    public void deleteComment(CommentId commentId, MemberId requesterId) {
        Comment comment = comments.stream()
                .filter(c -> commentId.equals(c.getId()))
                .findFirst()
                .orElseThrow(() -> new PostException("댓글을 찾을 수 없습니다: " + commentId.value()));

        if (!comment.getAuthorId().equals(requesterId)) {
            throw new PostException("댓글을 삭제할 권한이 없습니다");
        }
        comment.delete();
    }

    public void unreact(MemberId memberId) {
        boolean removed = reactions.removeIf(r -> r.memberId().equals(memberId));
        if (!removed) {
            throw new PostException("반응을 찾을 수 없습니다");
        }
    }

    public void update(String title, String content) {
        if (deleted) {
            throw new PostException("삭제된 게시글은 수정할 수 없습니다");
        }
        validateTitle(title);
        validateContent(content);
        this.title = title;
        this.content = content;
    }

    private static void validateTitle(String title) {
        Objects.requireNonNull(title, "게시글 제목은 필수입니다");
        if (title.isBlank()) throw new IllegalArgumentException("게시글 제목은 빈 값일 수 없습니다");
    }

    private static void validateContent(String content) {
        Objects.requireNonNull(content, "게시글 내용은 필수입니다");
        if (content.isBlank()) throw new IllegalArgumentException("게시글 내용은 빈 값일 수 없습니다");
    }

    public void delete() {
        if (deleted) {
            throw new IllegalStateException("이미 삭제된 게시글입니다");
        }
        this.deleted = true;
    }

    public long likeCount() {
        return reactions.stream().filter(r -> r.type() == ReactionType.LIKE).count();
    }

    public long dislikeCount() {
        return reactions.stream().filter(r -> r.type() == ReactionType.DISLIKE).count();
    }

    public PostId getId() { return id; }
    public BoardId getBoardId() { return boardId; }
    public MemberId getAuthorId() { return authorId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isDeleted() { return deleted; }
    public List<Comment> getComments() { return Collections.unmodifiableList(comments); }
    public List<Reaction> getReactions() { return Collections.unmodifiableList(reactions); }
}
