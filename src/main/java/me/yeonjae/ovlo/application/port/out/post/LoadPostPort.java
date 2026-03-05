package me.yeonjae.ovlo.application.port.out.post;

import me.yeonjae.ovlo.domain.post.model.Post;
import me.yeonjae.ovlo.domain.post.model.PostId;

import java.util.Optional;

public interface LoadPostPort {
    Optional<Post> findById(PostId postId);
}
