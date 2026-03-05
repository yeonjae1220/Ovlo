package me.yeonjae.ovlo.application.port.in.follow;

import me.yeonjae.ovlo.application.dto.result.FollowResult;

import java.util.List;

public interface GetFollowQuery {

    /** 나를 팔로우하는 사람 목록 */
    List<FollowResult> getFollowers(Long followeeId);

    /** 내가 팔로우하는 사람 목록 */
    List<FollowResult> getFollowings(Long followerId);
}
