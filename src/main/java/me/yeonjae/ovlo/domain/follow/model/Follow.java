package me.yeonjae.ovlo.domain.follow.model;

import me.yeonjae.ovlo.domain.member.model.MemberId;

import java.util.Objects;

public class Follow {

    private FollowId id;
    private MemberId followerId;
    private MemberId followeeId;

    private Follow() {}

    public static Follow create(MemberId followerId, MemberId followeeId) {
        Objects.requireNonNull(followerId, "팔로워 ID는 필수입니다");
        Objects.requireNonNull(followeeId, "팔로이 ID는 필수입니다");
        if (followerId.equals(followeeId)) {
            throw new IllegalArgumentException("자기 자신을 팔로우할 수 없습니다");
        }

        Follow follow = new Follow();
        follow.followerId = followerId;
        follow.followeeId = followeeId;
        return follow;
    }

    /** persistence 계층 전용: DB에서 모든 필드를 복원할 때 사용 */
    public static Follow restore(FollowId id, MemberId followerId, MemberId followeeId) {
        Follow follow = new Follow();
        follow.id = id;
        follow.followerId = followerId;
        follow.followeeId = followeeId;
        return follow;
    }

    public FollowId getId() { return id; }
    public MemberId getFollowerId() { return followerId; }
    public MemberId getFolloweeId() { return followeeId; }
}
