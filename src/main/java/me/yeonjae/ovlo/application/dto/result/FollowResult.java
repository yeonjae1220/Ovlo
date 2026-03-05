package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.follow.model.Follow;

public record FollowResult(
        Long followId,
        Long followerId,
        Long followeeId
) {
    public static FollowResult from(Follow follow) {
        return new FollowResult(
                follow.getId() != null ? follow.getId().value() : null,
                follow.getFollowerId().value(),
                follow.getFolloweeId().value()
        );
    }
}
