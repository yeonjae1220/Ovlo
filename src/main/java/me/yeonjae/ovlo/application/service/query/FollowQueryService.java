package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.MemberResult;
import me.yeonjae.ovlo.application.port.in.follow.GetFollowQuery;
import me.yeonjae.ovlo.application.port.out.follow.LoadFollowPort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FollowQueryService implements GetFollowQuery {

    private final LoadFollowPort loadFollowPort;
    private final LoadMemberPort loadMemberPort;

    public FollowQueryService(LoadFollowPort loadFollowPort, LoadMemberPort loadMemberPort) {
        this.loadFollowPort = loadFollowPort;
        this.loadMemberPort = loadMemberPort;
    }

    @Override
    public List<MemberResult> getFollowers(Long followeeId) {
        return loadFollowPort.findFollowersByFolloweeId(new MemberId(followeeId))
                .stream()
                .flatMap(follow -> loadMemberPort.findById(follow.getFollowerId()).stream())
                .map(MemberResult::from)
                .toList();
    }

    @Override
    public List<MemberResult> getFollowings(Long followerId) {
        return loadFollowPort.findFollowingsByFollowerId(new MemberId(followerId))
                .stream()
                .flatMap(follow -> loadMemberPort.findById(follow.getFolloweeId()).stream())
                .map(MemberResult::from)
                .toList();
    }
}
