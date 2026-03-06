package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.entity.PostJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.entity.PostReactionJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.mapper.PostMapper;
import me.yeonjae.ovlo.adapter.out.persistence.repository.CommentJpaRepository;
import me.yeonjae.ovlo.adapter.out.persistence.repository.PostJpaRepository;
import me.yeonjae.ovlo.adapter.out.persistence.repository.PostReactionJpaRepository;
import me.yeonjae.ovlo.application.port.out.post.LoadPostPort;
import me.yeonjae.ovlo.application.port.out.post.SavePostPort;
import me.yeonjae.ovlo.domain.board.model.BoardId;
import me.yeonjae.ovlo.domain.post.model.Comment;
import me.yeonjae.ovlo.domain.post.model.Post;
import me.yeonjae.ovlo.domain.post.model.PostId;
import me.yeonjae.ovlo.domain.post.model.Reaction;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public List<Post> findByBoardId(BoardId boardId, int offset, int limit) {
        // 쿼리 1: 게시글 페이지 로드 (최신순)
        PageRequest pageable = PageRequest.of(offset / limit, limit, Sort.by("id").descending());
        List<PostJpaEntity> entities = postJpaRepository.findByBoardIdAndDeletedFalse(boardId.value(), pageable);

        if (entities.isEmpty()) {
            return Collections.emptyList();
        }

        // 쿼리 2: 해당 페이지 게시글의 반응 배치 로드 (N+1 방지)
        List<Long> postIds = entities.stream().map(PostJpaEntity::getId).toList();
        Map<Long, List<PostReactionJpaEntity>> reactionsByPostId = postReactionJpaRepository
                .findByIdPostIdIn(postIds)
                .stream()
                .collect(Collectors.groupingBy(PostReactionJpaEntity::getPostId));

        return entities.stream()
                .map(e -> postMapper.toDomain(e,
                        Collections.emptyList(),
                        reactionsByPostId.getOrDefault(e.getId(), Collections.emptyList())))
                .toList();
    }

    @Override
    public long countByBoardId(BoardId boardId) {
        return postJpaRepository.countByBoardIdAndDeletedFalse(boardId.value());
    }

    @Override
    @Transactional
    public Post save(Post post) {
        var entity = postMapper.toJpaEntity(post);
        var saved = postJpaRepository.save(entity);
        Long postId = saved.getId();

        saveNewComments(postId, post.getComments());
        syncReactions(postId, post.getReactions());

        var comments = commentJpaRepository.findByPostId(postId);
        var reactions = postReactionJpaRepository.findByIdPostId(postId);
        return postMapper.toDomain(saved, comments, reactions);
    }

    private void saveNewComments(Long postId, List<Comment> comments) {
        comments.stream()
                .filter(c -> c.getId() == null)
                .forEach(c -> commentJpaRepository.save(postMapper.toCommentJpaEntity(postId, c)));
    }

    private void syncReactions(Long postId, List<Reaction> reactions) {
        postReactionJpaRepository.deleteByIdPostId(postId);
        List<PostReactionJpaEntity> entities = reactions.stream()
                .map(r -> postMapper.toReactionJpaEntity(postId, r))
                .toList();
        postReactionJpaRepository.saveAll(entities);
    }
}
