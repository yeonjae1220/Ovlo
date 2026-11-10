package me.yeonjae.ovlo.adapter.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "calendar_subscription") @IdClass(CalendarMembershipKey.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CalendarSubscriptionJpaEntity {
    @Id private Long memberId;
    @Id private Long universityId;
}
