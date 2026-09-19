package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.MemberSummaryResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.port.in.follow.GetFollowQuery;
import me.yeonjae.ovlo.application.port.out.follow.LoadFollowPort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.domain.follow.model.Follow;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
    public PageResult<MemberSummaryResult> getFollowers(Long followeeId, int page, int size) {
        MemberId id = new MemberId(followeeId);
        List<Follow> follows = loadFollowPort.findFollowersByFolloweeId(id, page * size, size);
        long total = loadFollowPort.countFollowersByFolloweeId(id);
        return PageResult.of(toSummaries(follows, Follow::getFollowerId), total, page, size);
    }

    @Override
    public PageResult<MemberSummaryResult> getFollowings(Long followerId, int page, int size) {
        MemberId id = new MemberId(followerId);
        List<Follow> follows = loadFollowPort.findFollowingsByFollowerId(id, page * size, size);
        long total = loadFollowPort.countFollowingsByFollowerId(id);
        return PageResult.of(toSummaries(follows, Follow::getFolloweeId), total, page, size);
    }

    @Override
    public List<Long> filterFollowing(Long followerId, List<Long> candidateIds) {
        if (candidateIds.isEmpty()) {
            return List.of();
        }
        List<MemberId> candidates = candidateIds.stream().map(MemberId::new).toList();
        return loadFollowPort.findFollowingIdsIn(new MemberId(followerId), candidates).stream()
                .map(MemberId::value)
                .toList();
    }

    /** 팔로우 행의 상대편 회원을 한 번에 로드해 팔로우 순서대로 요약 DTO 로 바꾼다. */
    private List<MemberSummaryResult> toSummaries(List<Follow> follows, Function<Follow, MemberId> counterpart) {
        List<MemberId> ids = follows.stream().map(counterpart).toList();
        Map<Long, Member> memberById = loadMemberPort.findAllByIds(ids).stream()
                .collect(Collectors.toMap(m -> m.getId().value(), m -> m));
        return follows.stream()
                .map(f -> memberById.get(counterpart.apply(f).value()))
                .filter(m -> m != null)
                .map(MemberSummaryResult::from)
                .toList();
    }
}
