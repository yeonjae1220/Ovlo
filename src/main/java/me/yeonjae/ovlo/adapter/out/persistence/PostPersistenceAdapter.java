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
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.post.model.Comment;
import me.yeonjae.ovlo.domain.post.model.Post;
import me.yeonjae.ovlo.domain.post.model.PostId;
import me.yeonjae.ovlo.domain.post.model.ReactionType;
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
        return postJpaRepository.findById(postId.value())
                .filter(entity -> !entity.isHiddenByWithdrawal())
                .map(entity -> {
                    var comments = commentJpaRepository.findByPostIdAndHiddenByWithdrawalFalse(entity.getId());
                    var reactions = postReactionJpaRepository.findByIdPostId(entity.getId());
                    return postMapper.toDomain(entity, comments, reactions);
                });
    }

    @Override
    public List<Post> findByBoardId(BoardId boardId, int offset, int limit) {
        // 쿼리 1: 게시글 페이지 로드 (최신순)
        PageRequest pageable = PageRequest.of(offset / limit, limit, Sort.by("id").descending());
        List<PostJpaEntity> entities = postJpaRepository.findByBoardIdAndDeletedFalseAndHiddenByWithdrawalFalse(boardId.value(), pageable);

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
        return postJpaRepository.countByBoardIdAndDeletedFalseAndHiddenByWithdrawalFalse(boardId.value());
    }

    @Override
    public List<Comment> findCommentsByPostId(PostId postId, int offset, int limit) {
        PageRequest pageable = PageRequest.of(offset / limit, limit, Sort.by("id").ascending());
        return commentJpaRepository.findByPostIdAndDeletedFalseAndHiddenByWithdrawalFalse(postId.value(), pageable)
                .stream()
                .map(postMapper::toCommentDomain)
                .toList();
    }

    @Override
    public long countCommentsByPostId(PostId postId) {
        return commentJpaRepository.countByPostIdAndDeletedFalseAndHiddenByWithdrawalFalse(postId.value());
    }

    @Override
    public List<Post> findAll(int offset, int limit) {
        PageRequest pageable = PageRequest.of(offset / limit, limit, Sort.by("id").descending());
        List<PostJpaEntity> entities = postJpaRepository.findAllByDeletedFalseAndHiddenByWithdrawalFalse(pageable);

        if (entities.isEmpty()) return Collections.emptyList();

        List<Long> postIds = entities.stream().map(PostJpaEntity::getId).toList();
        Map<Long, List<PostReactionJpaEntity>> reactionsByPostId = postReactionJpaRepository
                .findByIdPostIdIn(postIds)
                .stream()
                .collect(Collectors.groupingBy(PostReactionJpaEntity::getPostId));

        return entities.stream()
                .map(e -> postMapper.toDomain(e, Collections.emptyList(),
                        reactionsByPostId.getOrDefault(e.getId(), Collections.emptyList())))
                .toList();
    }

    @Override
    public long count() {
        return postJpaRepository.countByDeletedFalseAndHiddenByWithdrawalFalse();
    }

    @Override
    public List<Post> findByAuthorId(Long authorId, int offset, int limit) {
        PageRequest pageable = PageRequest.of(offset / limit, limit, Sort.by("id").descending());
        List<PostJpaEntity> entities = postJpaRepository
                .findByAuthorIdAndDeletedFalseAndHiddenByWithdrawalFalse(authorId, pageable);

        if (entities.isEmpty()) return Collections.emptyList();

        List<Long> postIds = entities.stream().map(PostJpaEntity::getId).toList();
        Map<Long, List<PostReactionJpaEntity>> reactionsByPostId = postReactionJpaRepository
                .findByIdPostIdIn(postIds)
                .stream()
                .collect(Collectors.groupingBy(PostReactionJpaEntity::getPostId));

        return entities.stream()
                .map(e -> postMapper.toDomain(e, Collections.emptyList(),
                        reactionsByPostId.getOrDefault(e.getId(), Collections.emptyList())))
                .toList();
    }

    @Override
    public long countByAuthorId(Long authorId) {
        return postJpaRepository.countByAuthorIdAndDeletedFalseAndHiddenByWithdrawalFalse(authorId);
    }

    @Override
    @Transactional
    public Post save(Post post) {
        // For existing posts, load the entity first to preserve the @Version value.
        // Creating a new PostJpaEntity with only the id set leaves version=null,
        // which causes Hibernate to throw PropertyValueException on save.
        PostJpaEntity entity;
        if (post.getId() != null) {
            entity = postJpaRepository.findById(post.getId().value())
                    .orElseThrow(() -> new IllegalStateException("Post not found: " + post.getId().value()));
            entity.setTitle(post.getTitle());
            entity.setContent(post.getContent());
            entity.setDeleted(post.isDeleted());
        } else {
            entity = postMapper.toJpaEntity(post);
        }
        var saved = postJpaRepository.save(entity);
        Long postId = saved.getId();

        saveAllComments(postId, post.getComments());
        // A안: 반응은 애그리거트 저장 경로가 아니라 upsertReaction/removeReaction 으로만 영속화한다.
        // save() 가 반응을 delete-all/re-insert 하면 불필요한 오버헤드에 더해, 동시 react 가 삽입한 행을
        // stale 스냅샷으로 덮어써 행-카운트 정합성이 깨진다. 그래서 여기서 반응은 건드리지 않는다.

        var comments = commentJpaRepository.findByPostIdAndHiddenByWithdrawalFalse(postId);
        var reactions = postReactionJpaRepository.findByIdPostId(postId);
        return postMapper.toDomain(saved, comments, reactions);
    }

    // JPA save()는 ID 유무에 따라 INSERT/UPDATE 자동 판단 — 신규/기존 구분 불필요
    private void saveAllComments(Long postId, List<Comment> comments) {
        comments.forEach(c -> commentJpaRepository.save(postMapper.toCommentJpaEntity(postId, c)));
    }

    /**
     * A안: 회원 1행 idempotent upsert + 비정규화 카운트 원자 증감.
     *
     * <p>해당 회원의 기존 행만 조회해 (없으면) 삽입, (반대 반응이면) 전환한다. 전체 애그리거트를
     * 다시 쓰지 않으므로 서로 다른 회원의 동시 반응은 서로 다른 행을 건드려 충돌하지 않는다. 카운트는
     * {@code UPDATE post SET like_count = like_count + :delta} 원자 증감이라 @Version/재시도가 필요 없다.
     */
    @Override
    @Transactional
    public void upsertReaction(PostId postId, MemberId memberId, ReactionType type) {
        Long pid = postId.value();
        Long mid = memberId.value();
        var id = new PostReactionJpaEntity.PostReactionId(pid, mid);

        var existing = postReactionJpaRepository.findById(id);
        if (existing.isEmpty()) {
            postReactionJpaRepository.save(new PostReactionJpaEntity(pid, mid, type));
            applyCountDelta(pid, type, +1);
            return;
        }

        PostReactionJpaEntity row = existing.get();
        ReactionType previous = row.getReactionType();
        if (previous == type) {
            return; // 완전 idempotent — 동일 반응 재요청은 무연산
        }
        row.setReactionType(type);
        postReactionJpaRepository.save(row);
        applyCountDelta(pid, previous, -1);
        applyCountDelta(pid, type, +1);
    }

    /** A안: 회원 1행 삭제 + 카운트 원자 감소. 반응이 없으면 무연산. */
    @Override
    @Transactional
    public void removeReaction(PostId postId, MemberId memberId) {
        Long pid = postId.value();
        var id = new PostReactionJpaEntity.PostReactionId(pid, memberId.value());
        postReactionJpaRepository.findById(id).ifPresent(row -> {
            postReactionJpaRepository.delete(row);
            applyCountDelta(pid, row.getReactionType(), -1);
        });
    }

    private void applyCountDelta(Long postId, ReactionType type, long delta) {
        if (type == ReactionType.LIKE) {
            postJpaRepository.addLikeCount(postId, delta);
        } else {
            postJpaRepository.addDislikeCount(postId, delta);
        }
    }
}
