package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.calendar.model.*;
public record CalendarEventResult(Long id, String kind, Long universityId, String universityName,
        EventDetails details, EventOrigin origin, EventStatus status, EventSource source, long version) {
    public static CalendarEventResult school(UniversityEvent e, String universityName) {
        return new CalendarEventResult(e.id(), "SCHOOL", e.universityId(), universityName, e.details(), e.origin(), e.status(), e.source(), e.version());
    }
    public static CalendarEventResult personal(PersonalEvent e) {
        return new CalendarEventResult(e.id(), "PERSONAL", null, null, e.details(), null, null, null, e.version());
    }
}
