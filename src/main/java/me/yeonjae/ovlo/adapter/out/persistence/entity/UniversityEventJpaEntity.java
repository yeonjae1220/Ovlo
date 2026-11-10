package me.yeonjae.ovlo.adapter.out.persistence.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import me.yeonjae.ovlo.domain.calendar.model.*;
import java.time.LocalDate;

@Entity @Table(name = "university_event") @Getter @Setter
public class UniversityEventJpaEntity extends CalendarEventFields {
    @Column(nullable = false) private Long universityId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private EventOrigin origin;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private EventStatus status;
    @Column(length = 2000) private String sourceUrl;
    private LocalDate sourceCheckedOn;
    @Column(nullable = false) private long createdBy;
    @Column(nullable = false) private long updatedBy;
    public UniversityEvent domain() {
        return UniversityEvent.restore(getId(), universityId, details(), origin, sourceUrl == null ? null : new EventSource(sourceUrl, sourceCheckedOn),
                status, createdBy, updatedBy, getVersion());
    }
}
