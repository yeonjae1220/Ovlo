package me.yeonjae.ovlo.application.port.in.follow;

import me.yeonjae.ovlo.application.dto.result.MemberSummaryResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;

import java.util.List;

public interface GetFollowQuery {

    /** 나를 팔로우하는 사람 목록 (공개 필드만) */
    PageResult<MemberSummaryResult> getFollowers(Long followeeId, int page, int size);

    /** 내가 팔로우하는 사람 목록 (공개 필드만) */
    PageResult<MemberSummaryResult> getFollowings(Long followerId, int page, int size);

    /** 후보 중 followerId 가 팔로우 중인 회원 ID — 목록 전체를 받지 않고 관계만 확인할 때 쓴다. */
    List<Long> filterFollowing(Long followerId, List<Long> candidateIds);
}
