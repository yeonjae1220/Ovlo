package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.MemberResult;
import me.yeonjae.ovlo.application.port.in.follow.GetFollowQuery;
import me.yeonjae.ovlo.application.port.out.follow.LoadFollowPort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.domain.follow.model.Follow;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        List<Follow> follows = loadFollowPort.findFollowersByFolloweeId(new MemberId(followeeId));
        List<MemberId> ids = follows.stream().map(Follow::getFollowerId).toList();
        var memberById = loadMemberPort.findAllByIds(ids).stream()
                .collect(Collectors.toMap(m -> m.getId().value(), m -> m));
        return follows.stream()
                .map(f -> memberById.get(f.getFollowerId().value()))
                .filter(m -> m != null)
                .map(MemberResult::from)
                .toList();
    }

    @Override
    public List<MemberResult> getFollowings(Long followerId) {
        List<Follow> follows = loadFollowPort.findFollowingsByFollowerId(new MemberId(followerId));
        List<MemberId> ids = follows.stream().map(Follow::getFolloweeId).toList();
        var memberById = loadMemberPort.findAllByIds(ids).stream()
                .collect(Collectors.toMap(m -> m.getId().value(), m -> m));
        return follows.stream()
                .map(f -> memberById.get(f.getFolloweeId().value()))
                .filter(m -> m != null)
                .map(MemberResult::from)
                .toList();
    }
}
