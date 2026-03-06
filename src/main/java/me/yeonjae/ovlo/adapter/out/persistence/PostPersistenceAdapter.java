package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.entity.PostReactionJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.mapper.PostMapper;
import me.yeonjae.ovlo.adapter.out.persistence.repository.CommentJpaRepository;
import me.yeonjae.ovlo.adapter.out.persistence.repository.PostJpaRepository;
import me.yeonjae.ovlo.adapter.out.persistence.repository.PostReactionJpaRepository;
import me.yeonjae.ovlo.application.port.out.post.LoadPostPort;
import me.yeonjae.ovlo.application.port.out.post.SavePostPort;
import me.yeonjae.ovlo.domain.post.model.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Component
public class PostPersistenceAdapter implements LoadPostPort, SavePostPort {

    private final PostJpaRepository postJpaRepository;
    private final CommentJpaRepository commentJpaRepository;
    private final PostReactionJpaRepository postReactionJpaRepository;
    private final PostMapper postMapper;

    public PostPersistenceAdapter(PostJpaRepository postJpaRepository,
                                  CommentJpaRepository commentJpaRepository,
                                  PostReactionJpaRepository postReactionJpaRepository,
                                  PostMapper postMapper) {
        this.postJpaRepository = postJpaRepository;
        this.commentJpaRepository = commentJpaRepository;
        this.postReactionJpaRepository = postReactionJpaRepository;
        this.postMapper = postMapper;
    }

    @Override
    public Optional<Post> findById(PostId postId) {
        return postJpaRepository.findById(postId.value()).map(entity -> {
            var comments = commentJpaRepository.findByPostId(entity.getId());
            var reactions = postReactionJpaRepository.findByIdPostId(entity.getId());
            return postMapper.toDomain(entity, comments, reactions);
        });
    }

    @Override
    @Transactional
    public Post save(Post post) {
        var entity = postMapper.toJpaEntity(post);
        var saved = postJpaRepository.save(entity);
        Long postId = saved.getId();

        // Save new comments (id == null)
        post.getComments().stream()
                .filter(c -> c.getId() == null)
                .forEach(c -> commentJpaRepository.save(postMapper.toCommentJpaEntity(postId, c)));

        // Sync reactions: delete all and re-insert
        postReactionJpaRepository.deleteByIdPostId(postId);
        List<PostReactionJpaEntity> reactionEntities = post.getReactions().stream()
                .map(r -> postMapper.toReactionJpaEntity(postId, r))
                .toList();
        postReactionJpaRepository.saveAll(reactionEntities);

        var comments = commentJpaRepository.findByPostId(postId);
        var reactions = postReactionJpaRepository.findByIdPostId(postId);
        return postMapper.toDomain(saved, comments, reactions);
    }
}
