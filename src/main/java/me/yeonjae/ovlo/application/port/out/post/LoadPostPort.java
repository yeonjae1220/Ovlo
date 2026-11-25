package me.yeonjae.ovlo.application.port.out.post;

import me.yeonjae.ovlo.domain.board.model.BoardId;
import me.yeonjae.ovlo.domain.post.model.Comment;
import me.yeonjae.ovlo.domain.post.model.Post;
import me.yeonjae.ovlo.domain.post.model.PostId;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface LoadPostPort {
    java.util.Map<Long, Long> countComments(List<Long> postIds, Set<Long> excludedAuthorIds);
    Optional<Post> findById(PostId postId);
    // excludedAuthorIds: 조회하는 회원과 차단 관계인 작성자. 비어 있으면 거르지 않는다.
    List<Post> findByBoardId(BoardId boardId, Set<Long> excludedAuthorIds, int offset, int limit);
    long countByBoardId(BoardId boardId, Set<Long> excludedAuthorIds);
    List<Comment> findCommentsByPostId(PostId postId, Set<Long> excludedAuthorIds, int offset, int limit);
    long countCommentsByPostId(PostId postId, Set<Long> excludedAuthorIds);
    List<Post> findAll(Set<Long> excludedAuthorIds, int offset, int limit);
    long count(Set<Long> excludedAuthorIds);
    /** 관리자용 — 차단과 무관하게 전체 */
    List<Post> findAll(int offset, int limit);
    long count();
    List<Post> findByAuthorId(Long authorId, int offset, int limit);
    long countByAuthorId(Long authorId);
}
