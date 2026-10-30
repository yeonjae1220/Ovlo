package me.yeonjae.ovlo.adapter.out.persistence.repository;

import me.yeonjae.ovlo.adapter.out.persistence.entity.OfficeJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OfficeJpaRepository extends JpaRepository<OfficeJpaEntity, Long> {

    @EntityGraph(attributePaths = "staff")
    Optional<OfficeJpaEntity> findWithStaffById(Long id);

    @EntityGraph(attributePaths = "staff")
    Optional<OfficeJpaEntity> findWithStaffByGlobalUnivId(Long globalUnivId);

    /** 이 회원에게 INVITED 초대가 있는 Office (여럿이면 가장 오래된 초대 순). */
    @Query("""
            SELECT DISTINCT o FROM OfficeJpaEntity o
              JOIN FETCH o.staff s
             WHERE s.memberId = :memberId AND s.status = 'INVITED'
             ORDER BY o.id ASC
            """)
    List<OfficeJpaEntity> findWithInvitationFor(@Param("memberId") Long memberId);

    /**
     * 활동 담당자 소속 프로젝션: [officeId, role]. 정지된 Office 는 제외. 수락 순.
     * 토큰 발급 경로에서 매번 돌므로 엔티티를 로딩하지 않는다.
     */
    @Query("""
            SELECT s.office.id, s.role FROM OfficeStaffJpaEntity s
             WHERE s.memberId = :memberId
               AND s.status = 'ACTIVE'
               AND s.office.status <> 'SUSPENDED'
             ORDER BY s.acceptedAt ASC, s.id ASC
            """)
    List<Object[]> findActiveMembershipRows(@Param("memberId") Long memberId);
}
