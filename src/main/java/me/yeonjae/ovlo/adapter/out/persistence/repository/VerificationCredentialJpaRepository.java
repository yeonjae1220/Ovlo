package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.VerificationCredentialJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface VerificationCredentialJpaRepository extends JpaRepository<VerificationCredentialJpaEntity, Long> {

    List<VerificationCredentialJpaEntity> findByMemberId(Long memberId);

    void deleteByMemberIdAndType(Long memberId, String type);

    /** verifiedAt이 cutoff 이전인 VERIFIED 자격을 EXPIRED로 일괄 전환. 반환=영향 행 수. */
    @Modifying
    @Query("""
            UPDATE VerificationCredentialJpaEntity c
               SET c.status = 'EXPIRED'
             WHERE c.status = 'VERIFIED'
               AND c.verifiedAt < :cutoff
            """)
    int expireVerifiedOlderThan(@Param("cutoff") Instant cutoff);

    @Query("""
            SELECT COUNT(c) > 0 FROM VerificationCredentialJpaEntity c
            WHERE lower(c.verifiedEmail) = lower(:email)
              AND c.status = 'VERIFIED'
              AND c.memberId <> :memberId
            """)
    boolean existsActiveByVerifiedEmailAndMemberIdNot(@Param("email") String email,
                                                      @Param("memberId") Long memberId);

    /**
     * 단일 자격을 EXPIRED로 전환(관리자 취소)하고 취소자/취소시각을 기록한다.
     * memberId로 소유 범위를 한정해 URL 조작으로 타 회원 자격을 취소하지 못하게 한다.
     * 반환=영향 행 수(0이면 미존재/불일치/이미 만료).
     */
    @Modifying
    @Query("""
            UPDATE VerificationCredentialJpaEntity c
               SET c.status = 'EXPIRED',
                   c.revokedBy = :revokedBy,
                   c.revokedAt = :revokedAt
             WHERE c.id = :id
               AND c.memberId = :memberId
               AND c.status = 'VERIFIED'
            """)
    int revokeByIdAndMemberId(@Param("id") Long id, @Param("memberId") Long memberId,
                              @Param("revokedBy") String revokedBy, @Param("revokedAt") Instant revokedAt);
}
