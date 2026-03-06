package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.mapper.FollowMapper;
import me.yeonjae.ovlo.adapter.out.persistence.repository.FollowJpaRepository;
import me.yeonjae.ovlo.application.port.out.follow.LoadFollowPort;
import me.yeonjae.ovlo.application.port.out.follow.SaveFollowPort;
import me.yeonjae.ovlo.domain.follow.model.Follow;
import me.yeonjae.ovlo.domain.member.model.MemberId;
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
        return followJpaRepository.findByFollowerIdAndFolloweeId(followerId.value(), followeeId.value())
                .map(followMapper::toDomain);
    }

    @Override
    public boolean existsByFollowerAndFollowee(MemberId followerId, MemberId followeeId) {
        return followJpaRepository.existsByFollowerIdAndFolloweeId(followerId.value(), followeeId.value());
    }

    @Override
    public List<Follow> findFollowersByFolloweeId(MemberId followeeId) {
        return followJpaRepository.findByFolloweeId(followeeId.value()).stream()
                .map(followMapper::toDomain).toList();
    }

    @Override
    public List<Follow> findFollowingsByFollowerId(MemberId followerId) {
        return followJpaRepository.findByFollowerId(followerId.value()).stream()
                .map(followMapper::toDomain).toList();
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
