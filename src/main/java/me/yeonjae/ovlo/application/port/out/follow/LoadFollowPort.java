package me.yeonjae.ovlo.application.port.out.follow;

import me.yeonjae.ovlo.domain.follow.model.Follow;
import me.yeonjae.ovlo.domain.member.model.MemberId;

import java.util.List;
import java.util.Optional;

public interface LoadFollowPort {

    Optional<Follow> findByFollowerAndFollowee(MemberId followerId, MemberId followeeId);

    boolean existsByFollowerAndFollowee(MemberId followerId, MemberId followeeId);

    List<Follow> findFollowersByFolloweeId(MemberId followeeId, int offset, int limit);

    long countFollowersByFolloweeId(MemberId followeeId);

    List<Follow> findFollowingsByFollowerId(MemberId followerId, int offset, int limit);

    long countFollowingsByFollowerId(MemberId followerId);

    /** 후보 중 followerId 가 실제로 팔로우 중인 회원 ID — 관계 확인 전용(회원 정보를 싣지 않는다). */
    List<MemberId> findFollowingIdsIn(MemberId followerId, List<MemberId> candidateIds);
}
