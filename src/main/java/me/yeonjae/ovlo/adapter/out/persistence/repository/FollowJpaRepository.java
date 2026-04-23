package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.FollowJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowJpaRepository extends JpaRepository<FollowJpaEntity, Long> {
    Optional<FollowJpaEntity> findByFollowerIdAndFolloweeIdAndHiddenByWithdrawalFalse(Long followerId, Long followeeId);
    boolean existsByFollowerIdAndFolloweeIdAndHiddenByWithdrawalFalse(Long followerId, Long followeeId);
    List<FollowJpaEntity> findByFolloweeIdAndHiddenByWithdrawalFalse(Long followeeId);
    List<FollowJpaEntity> findByFollowerIdAndHiddenByWithdrawalFalse(Long followerId);
    Optional<FollowJpaEntity> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    @Modifying
    @Query("UPDATE FollowJpaEntity f SET f.hiddenByWithdrawal = true WHERE f.followerId = :memberId OR f.followeeId = :memberId")
    void hideAllByMemberId(@Param("memberId") Long memberId);
}
