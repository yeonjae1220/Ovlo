package me.yeonjae.ovlo.application.port.out.post;

import me.yeonjae.ovlo.domain.board.model.BoardId;
import me.yeonjae.ovlo.domain.post.model.Comment;
import me.yeonjae.ovlo.domain.post.model.Post;
import me.yeonjae.ovlo.domain.post.model.PostId;

import java.util.List;
import java.util.Optional;

public interface LoadPostPort {
    Optional<Post> findById(PostId postId);
    List<Post> findByBoardId(BoardId boardId, int offset, int limit);
    long countByBoardId(BoardId boardId);
    List<Comment> findCommentsByPostId(PostId postId, int offset, int limit);
    long countCommentsByPostId(PostId postId);
}
