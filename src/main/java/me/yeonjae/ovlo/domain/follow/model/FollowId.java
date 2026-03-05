package me.yeonjae.ovlo.domain.follow.model;

import java.util.Objects;

public record FollowId(Long value) {

    public FollowId {
        Objects.requireNonNull(value, "FollowId는 null일 수 없습니다");
    }
}
