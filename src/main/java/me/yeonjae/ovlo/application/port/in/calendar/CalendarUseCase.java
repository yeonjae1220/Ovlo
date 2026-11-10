package me.yeonjae.ovlo.application.port.in.calendar;
import me.yeonjae.ovlo.application.dto.command.*;
import me.yeonjae.ovlo.application.dto.result.CalendarEventResult;
public interface CalendarUseCase {
    CalendarEventResult createPersonal(long memberId, SaveCalendarEventCommand command);
    CalendarEventResult updatePersonal(long memberId, long id, SaveCalendarEventCommand command);
    void deletePersonal(long memberId, long id, long version);
    CalendarEventResult createSchool(CalendarActor actor, long schoolId, SaveCalendarEventCommand command);
    CalendarEventResult updateSchool(CalendarActor actor, long id, SaveCalendarEventCommand command);
    CalendarEventResult changeStatus(CalendarActor actor, long id, String action, long version);
    void deleteDraft(CalendarActor actor, long id, long version);
    void subscribe(long memberId, long schoolId, boolean subscribed);
}
