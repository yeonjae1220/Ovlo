package me.yeonjae.ovlo.adapter.out.persistence.mapper;

import me.yeonjae.ovlo.adapter.out.persistence.entity.CommentJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.entity.PostJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.entity.PostReactionJpaEntity;
import me.yeonjae.ovlo.domain.board.model.BoardId;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.post.model.*;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class PostMapper {

    public PostJpaEntity toJpaEntity(Post post) {
        PostJpaEntity entity = new PostJpaEntity();
        if (post.getId() != null) {
            entity.setId(post.getId().value());
        }
        entity.setBoardId(post.getBoardId().value());
        entity.setAuthorId(post.getAuthorId().value());
        entity.setTitle(post.getTitle());
        entity.setContent(post.getContent());
        entity.setDeleted(post.isDeleted());
        return entity;
    }

    public Comment toCommentDomain(CommentJpaEntity entity) {
        return Comment.restore(
                new CommentId(entity.getId()),
                new PostId(entity.getPostId()),
                new MemberId(entity.getAuthorId()),
                entity.getContent(),
                entity.isDeleted());
    }

    public CommentJpaEntity toCommentJpaEntity(Long postId, Comment comment) {
        CommentJpaEntity entity = new CommentJpaEntity();
        if (comment.getId() != null) {
            entity.setId(comment.getId().value());
        }
        entity.setPostId(postId);
        entity.setAuthorId(comment.getAuthorId().value());
        entity.setContent(comment.getContent());
        entity.setDeleted(comment.isDeleted());
        return entity;
    }

    public PostReactionJpaEntity toReactionJpaEntity(Long postId, Reaction reaction) {
        return new PostReactionJpaEntity(postId, reaction.memberId().value(), reaction.type());
    }

    public Post toDomain(PostJpaEntity entity, List<CommentJpaEntity> comments, List<PostReactionJpaEntity> reactions) {
        List<Comment> domainComments = comments.stream()
                .map(c -> Comment.restore(
                        new CommentId(c.getId()),
                        new PostId(c.getPostId()),
                        new MemberId(c.getAuthorId()),
                        c.getContent(),
                        c.isDeleted()))
                .toList();

        List<Reaction> domainReactions = reactions.stream()
                .map(r -> new Reaction(new MemberId(r.getMemberId()), r.getReactionType()))
                .toList();

        return Post.restore(
                new PostId(entity.getId()),
                new BoardId(entity.getBoardId()),
                new MemberId(entity.getAuthorId()),
                entity.getTitle(),
                entity.getContent(),
                entity.isDeleted(),
                domainComments,
                domainReactions,
                entity.getCreatedAt()
        );
    }
}
