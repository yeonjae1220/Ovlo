package me.yeonjae.ovlo.adapter.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "university_calendar_manager") @IdClass(CalendarMembershipKey.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UniversityCalendarManagerJpaEntity {
    @Id private Long memberId;
    @Id private Long universityId;
}
