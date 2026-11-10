package me.yeonjae.ovlo.adapter.out.persistence.repository;
import me.yeonjae.ovlo.adapter.out.persistence.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface UniversityCalendarManagerJpaRepository extends JpaRepository<UniversityCalendarManagerJpaEntity, CalendarMembershipKey> {
    List<UniversityCalendarManagerJpaEntity> findByMemberId(long memberId);
}
