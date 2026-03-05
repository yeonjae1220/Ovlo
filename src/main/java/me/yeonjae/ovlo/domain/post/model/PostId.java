package me.yeonjae.ovlo.domain.post.model;

import java.util.Objects;

public record PostId(Long value) {

    public PostId {
        Objects.requireNonNull(value, "PostId는 null일 수 없습니다");
    }
}
