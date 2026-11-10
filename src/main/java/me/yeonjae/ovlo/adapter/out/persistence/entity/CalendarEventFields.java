package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import me.yeonjae.ovlo.domain.calendar.model.*;
import java.time.*;

@MappedSuperclass @Getter @Setter
public abstract class CalendarEventFields {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @Column(nullable = false, length = 200) private String title;
    @Column(columnDefinition = "text") private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private EventCategory category;
    @Column(nullable = false) private boolean allDay;
    private LocalDate startDate;
    private LocalDate endDate;
    private Instant startsAt;
    private Instant endsAt;
    @Column(nullable = false, length = 100) private String timeZone;
    @Column(length = 300) private String location;
    @Column(length = 2000) private String actionUrl;
    private Instant deadlineAt;
    @Column(length = 500) private String audience;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @PrePersist void insertTimes() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void updateTime() { updatedAt = Instant.now(); }
    public void apply(EventDetails d) {
        title = d.title(); description = d.description(); category = d.category(); location = d.location();
        actionUrl = d.actionUrl(); deadlineAt = d.deadlineAt(); audience = d.audience();
        var p = d.period(); allDay = p.allDay(); startDate = p.startDate(); endDate = p.endDate();
        startsAt = p.startsAt(); endsAt = p.endsAt(); timeZone = p.timeZone();
    }
    public EventDetails details() {
        return new EventDetails(title, description, category, new EventPeriod(allDay, startDate, endDate, startsAt, endsAt, timeZone), location, actionUrl, deadlineAt, audience);
    }
}
