package me.yeonjae.ovlo.domain.post.model;

import java.util.Objects;

public record CommentId(Long value) {

    public CommentId {
        Objects.requireNonNull(value, "CommentId는 null일 수 없습니다");
    }
}
