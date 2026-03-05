package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.port.out.post.LoadPostPort;
import me.yeonjae.ovlo.application.port.out.post.SavePostPort;
import me.yeonjae.ovlo.domain.post.model.Post;
import me.yeonjae.ovlo.domain.post.model.PostId;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Post JPA 구현 예정 (stub).
 * LoadPostPort / SavePostPort 구현.
 */
@Component
public class PostPersistenceAdapter implements LoadPostPort, SavePostPort {

    @Override
    public Optional<Post> findById(PostId postId) {
        throw new UnsupportedOperationException("Post JPA 구현 예정");
    }

    @Override
    public Post save(Post post) {
        throw new UnsupportedOperationException("Post JPA 구현 예정");
    }
}
