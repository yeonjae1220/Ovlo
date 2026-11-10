package me.yeonjae.ovlo.adapter.out.persistence.repository;
import me.yeonjae.ovlo.adapter.out.persistence.entity.PersonalEventJpaEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import java.time.*;
import java.util.*;
public interface PersonalEventJpaRepository extends JpaRepository<PersonalEventJpaEntity, Long> {
    Optional<PersonalEventJpaEntity> findByIdAndMemberId(long id, long memberId);
    @Query("""
        select e from PersonalEventJpaEntity e where e.memberId = :memberId
        and ((e.allDay = true and e.startDate < :to and e.endDate > :from)
          or (e.allDay = false and e.startsAt < :end and e.endsAt > :start)) order by e.id
        """)
    List<PersonalEventJpaEntity> overlapping(long memberId, LocalDate from, LocalDate to, Instant start, Instant end, Pageable pageable);
}
