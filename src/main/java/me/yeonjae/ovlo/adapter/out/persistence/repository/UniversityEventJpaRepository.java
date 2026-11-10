package me.yeonjae.ovlo.adapter.out.persistence.repository;
import me.yeonjae.ovlo.adapter.out.persistence.entity.UniversityEventJpaEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import java.time.*;
import java.util.List;
public interface UniversityEventJpaRepository extends JpaRepository<UniversityEventJpaEntity, Long> {
    @Query("""
        select e from UniversityEventJpaEntity e where e.universityId in :schools
        and (:drafts = true or e.status <> me.yeonjae.ovlo.domain.calendar.model.EventStatus.DRAFT)
        and ((e.allDay = true and e.startDate < :to and e.endDate > :from)
          or (e.allDay = false and e.startsAt < :end and e.endsAt > :start)) order by e.id
        """)
    List<UniversityEventJpaEntity> overlapping(List<Long> schools, LocalDate from, LocalDate to, Instant start, Instant end, boolean drafts, Pageable pageable);
}
