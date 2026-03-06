package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.FollowResult;
import me.yeonjae.ovlo.application.port.in.follow.GetFollowQuery;
import me.yeonjae.ovlo.application.port.out.follow.LoadFollowPort;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FollowQueryService implements GetFollowQuery {

    private final LoadFollowPort loadFollowPort;

    public FollowQueryService(LoadFollowPort loadFollowPort) {
        this.loadFollowPort = loadFollowPort;
    }

    @Override
    public List<FollowResult> getFollowers(Long followeeId) {
        return loadFollowPort.findFollowersByFolloweeId(new MemberId(followeeId))
                .stream()
                .map(FollowResult::from)
                .toList();
    }

    @Override
    public List<FollowResult> getFollowings(Long followerId) {
        return loadFollowPort.findFollowingsByFollowerId(new MemberId(followerId))
                .stream()
                .map(FollowResult::from)
                .toList();
    }
}
