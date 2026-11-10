package me.yeonjae.ovlo.adapter.out.persistence.repository;
import me.yeonjae.ovlo.adapter.out.persistence.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CalendarSubscriptionJpaRepository extends JpaRepository<CalendarSubscriptionJpaEntity, CalendarMembershipKey> {
    List<CalendarSubscriptionJpaEntity> findByMemberId(long memberId);
}
