package me.yeonjae.ovlo.adapter.out.persistence.repository;
import me.yeonjae.ovlo.adapter.out.persistence.entity.UniversityApplicationJpaEntity;
import me.yeonjae.ovlo.domain.calendar.model.ApplicationStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import java.util.List;
public interface UniversityApplicationJpaRepository extends JpaRepository<UniversityApplicationJpaEntity, Long> {
    boolean existsByMemberIdAndUniversityIdAndActiveSlotTrue(long memberId, long universityId);
    @Query("select a from UniversityApplicationJpaEntity a where (:memberId is null or a.memberId = :memberId) and (:status is null or a.status = :status) order by a.id desc")
    List<UniversityApplicationJpaEntity> search(Long memberId, ApplicationStatus status, Pageable pageable);
}
