package me.yeonjae.ovlo.application.port.out.calendar;
import me.yeonjae.ovlo.domain.calendar.model.*;
import java.time.*;
import java.util.*;
public interface CalendarEventPort {
    Optional<UniversityEvent> findSchoolEvent(long id);
    Optional<PersonalEvent> findPersonalEvent(long id, long memberId);
    UniversityEvent saveSchoolEvent(UniversityEvent event, String actor);
    PersonalEvent savePersonalEvent(PersonalEvent event);
    void deletePersonalEvent(PersonalEvent event);
    void deleteSchoolDraft(UniversityEvent event, String actor);
    List<UniversityEvent> schoolEvents(List<Long> schools, LocalDate from, LocalDate to, Instant start, Instant end, boolean includeDrafts, int limit);
    List<PersonalEvent> personalEvents(long memberId, LocalDate from, LocalDate to, Instant start, Instant end, int limit);
}
