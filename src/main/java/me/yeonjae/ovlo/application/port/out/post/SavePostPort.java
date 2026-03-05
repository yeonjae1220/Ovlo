package me.yeonjae.ovlo.application.port.out.post;

import me.yeonjae.ovlo.domain.post.model.Post;

public interface SavePostPort {
    Post save(Post post);
}
