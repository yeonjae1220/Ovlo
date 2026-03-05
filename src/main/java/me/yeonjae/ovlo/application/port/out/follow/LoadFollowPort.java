package me.yeonjae.ovlo.application.port.out.follow;

import me.yeonjae.ovlo.domain.follow.model.Follow;
import me.yeonjae.ovlo.domain.member.model.MemberId;

import java.util.List;
import java.util.Optional;

public interface LoadFollowPort {

    Optional<Follow> findByFollowerAndFollowee(MemberId followerId, MemberId followeeId);

    boolean existsByFollowerAndFollowee(MemberId followerId, MemberId followeeId);

    List<Follow> findFollowersByFolloweeId(MemberId followeeId);

    List<Follow> findFollowingsByFollowerId(MemberId followerId);
}
