package me.yeonjae.ovlo.application.port.in.calendar;
import me.yeonjae.ovlo.application.dto.command.CalendarActor;
import me.yeonjae.ovlo.application.dto.result.*;
import java.time.LocalDate;
import java.util.List;
public interface CalendarQuery {
    CalendarOptionsResult options(long memberId);
    List<CalendarEventResult> calendar(long memberId, LocalDate from, LocalDate to, String timeZone);
    List<CalendarEventResult> management(CalendarActor actor, long schoolId, LocalDate from, LocalDate to, String timeZone);
}
