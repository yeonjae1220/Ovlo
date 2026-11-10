package me.yeonjae.ovlo.adapter.out.persistence.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import me.yeonjae.ovlo.domain.calendar.model.PersonalEvent;
@Entity @Table(name = "personal_event") @Getter @Setter
public class PersonalEventJpaEntity extends CalendarEventFields {
    @Column(nullable = false) private Long memberId;
    public PersonalEvent domain() { return PersonalEvent.restore(getId(), memberId, details(), getVersion()); }
}
