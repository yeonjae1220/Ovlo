package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.VerificationCredentialJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VerificationCredentialJpaRepository extends JpaRepository<VerificationCredentialJpaEntity, Long> {

    List<VerificationCredentialJpaEntity> findByMemberId(Long memberId);

    void deleteByMemberIdAndType(Long memberId, String type);

    @Query("""
            SELECT COUNT(c) > 0 FROM VerificationCredentialJpaEntity c
            WHERE lower(c.verifiedEmail) = lower(:email)
              AND c.status = 'VERIFIED'
              AND c.memberId <> :memberId
            """)
    boolean existsActiveByVerifiedEmailAndMemberIdNot(@Param("email") String email,
                                                      @Param("memberId") Long memberId);
}
