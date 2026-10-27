package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.FollowJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowJpaRepository extends JpaRepository<FollowJpaEntity, Long> {
    Optional<FollowJpaEntity> findByFollowerIdAndFolloweeIdAndHiddenByWithdrawalFalse(Long followerId, Long followeeId);
    boolean existsByFollowerIdAndFolloweeIdAndHiddenByWithdrawalFalse(Long followerId, Long followeeId);
    List<FollowJpaEntity> findByFolloweeIdAndHiddenByWithdrawalFalse(Long followeeId, Pageable pageable);
    long countByFolloweeIdAndHiddenByWithdrawalFalse(Long followeeId);
    List<FollowJpaEntity> findByFollowerIdAndHiddenByWithdrawalFalse(Long followerId, Pageable pageable);
    long countByFollowerIdAndHiddenByWithdrawalFalse(Long followerId);

    /** 관계 확인 전용 — 후보 중 실제 팔로우 행만 돌려준다(회원 테이블 미조회). */
    List<FollowJpaEntity> findByFollowerIdAndFolloweeIdInAndHiddenByWithdrawalFalse(Long followerId, List<Long> followeeIds);
    Optional<FollowJpaEntity> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    @Modifying
    @Query("UPDATE FollowJpaEntity f SET f.hiddenByWithdrawal = true WHERE f.followerId = :memberId OR f.followeeId = :memberId")
    void hideAllByMemberId(@Param("memberId") Long memberId);
}
