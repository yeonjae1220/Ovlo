package me.yeonjae.ovlo.adapter.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name = "calendar_audit") @Getter @Setter
public class CalendarAuditJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 30) private String aggregateType;
    @Column(nullable = false) private Long aggregateId;
    @Column(nullable = false) private String actor;
    @Column(nullable = false, length = 30) private String action;
    @Column(nullable = false, columnDefinition = "text") private String snapshot;
    @Column(nullable = false) private Instant createdAt;
}
