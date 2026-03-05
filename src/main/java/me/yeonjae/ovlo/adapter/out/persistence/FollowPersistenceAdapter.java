package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.port.out.follow.LoadFollowPort;
import me.yeonjae.ovlo.application.port.out.follow.SaveFollowPort;
import me.yeonjae.ovlo.domain.follow.model.Follow;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Follow JPA 구현 예정 (stub).
 * LoadFollowPort / SaveFollowPort 구현.
 */
@Component
public class FollowPersistenceAdapter implements LoadFollowPort, SaveFollowPort {

    @Override
    public Optional<Follow> findByFollowerAndFollowee(MemberId followerId, MemberId followeeId) {
        throw new UnsupportedOperationException("Follow JPA 구현 예정");
    }

    @Override
    public boolean existsByFollowerAndFollowee(MemberId followerId, MemberId followeeId) {
        throw new UnsupportedOperationException("Follow JPA 구현 예정");
    }

    @Override
    public List<Follow> findFollowersByFolloweeId(MemberId followeeId) {
        throw new UnsupportedOperationException("Follow JPA 구현 예정");
    }

    @Override
    public List<Follow> findFollowingsByFollowerId(MemberId followerId) {
        throw new UnsupportedOperationException("Follow JPA 구현 예정");
    }

    @Override
    public Follow save(Follow follow) {
        throw new UnsupportedOperationException("Follow JPA 구현 예정");
    }

    @Override
    public void delete(Follow follow) {
        throw new UnsupportedOperationException("Follow JPA 구현 예정");
    }
}
