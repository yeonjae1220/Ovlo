package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.FollowJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FollowJpaRepository extends JpaRepository<FollowJpaEntity, Long> {
    Optional<FollowJpaEntity> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    List<FollowJpaEntity> findByFolloweeId(Long followeeId);
    List<FollowJpaEntity> findByFollowerId(Long followerId);
}
