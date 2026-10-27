package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.mapper.FollowMapper;
import me.yeonjae.ovlo.adapter.out.persistence.repository.FollowJpaRepository;
import me.yeonjae.ovlo.application.port.out.follow.LoadFollowPort;
import me.yeonjae.ovlo.application.port.out.follow.SaveFollowPort;
import me.yeonjae.ovlo.domain.follow.model.Follow;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class FollowPersistenceAdapter implements LoadFollowPort, SaveFollowPort {

    private final FollowJpaRepository followJpaRepository;
    private final FollowMapper followMapper;

    public FollowPersistenceAdapter(FollowJpaRepository followJpaRepository, FollowMapper followMapper) {
        this.followJpaRepository = followJpaRepository;
        this.followMapper = followMapper;
    }

    @Override
    public Optional<Follow> findByFollowerAndFollowee(MemberId followerId, MemberId followeeId) {
        return followJpaRepository.findByFollowerIdAndFolloweeIdAndHiddenByWithdrawalFalse(followerId.value(), followeeId.value())
                .map(followMapper::toDomain);
    }

    @Override
    public boolean existsByFollowerAndFollowee(MemberId followerId, MemberId followeeId) {
        return followJpaRepository.existsByFollowerIdAndFolloweeIdAndHiddenByWithdrawalFalse(followerId.value(), followeeId.value());
    }

    @Override
    public List<Follow> findFollowersByFolloweeId(MemberId followeeId, int offset, int limit) {
        return followJpaRepository.findByFolloweeIdAndHiddenByWithdrawalFalse(followeeId.value(), recentFirst(offset, limit))
                .stream().map(followMapper::toDomain).toList();
    }

    @Override
    public long countFollowersByFolloweeId(MemberId followeeId) {
        return followJpaRepository.countByFolloweeIdAndHiddenByWithdrawalFalse(followeeId.value());
    }

    @Override
    public List<Follow> findFollowingsByFollowerId(MemberId followerId, int offset, int limit) {
        return followJpaRepository.findByFollowerIdAndHiddenByWithdrawalFalse(followerId.value(), recentFirst(offset, limit))
                .stream().map(followMapper::toDomain).toList();
    }

    @Override
    public long countFollowingsByFollowerId(MemberId followerId) {
        return followJpaRepository.countByFollowerIdAndHiddenByWithdrawalFalse(followerId.value());
    }

    @Override
    public List<MemberId> findFollowingIdsIn(MemberId followerId, List<MemberId> candidateIds) {
        List<Long> ids = candidateIds.stream().map(MemberId::value).toList();
        return followJpaRepository
                .findByFollowerIdAndFolloweeIdInAndHiddenByWithdrawalFalse(followerId.value(), ids)
                .stream().map(e -> new MemberId(e.getFolloweeId())).toList();
    }

    /** 최근 맺은 관계부터. offset/limit 은 호출부 규약(PostPersistenceAdapter 와 동일). */
    private static PageRequest recentFirst(int offset, int limit) {
        return PageRequest.of(limit > 0 ? offset / limit : 0, limit, Sort.by("id").descending());
    }

    @Override
    public Follow save(Follow follow) {
        return followMapper.toDomain(followJpaRepository.save(followMapper.toJpaEntity(follow)));
    }

    @Override
    public void delete(Follow follow) {
        followJpaRepository.findByFollowerIdAndFolloweeId(
                follow.getFollowerId().value(), follow.getFolloweeId().value())
                .ifPresent(followJpaRepository::delete);
    }
}
