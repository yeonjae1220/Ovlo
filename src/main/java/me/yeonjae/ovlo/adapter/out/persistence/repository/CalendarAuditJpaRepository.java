package me.yeonjae.ovlo.adapter.out.persistence.repository;
import me.yeonjae.ovlo.adapter.out.persistence.entity.CalendarAuditJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CalendarAuditJpaRepository extends JpaRepository<CalendarAuditJpaEntity, Long> {}
