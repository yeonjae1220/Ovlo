package me.yeonjae.ovlo.application.port.in.calendar;
import me.yeonjae.ovlo.application.dto.command.*;
import me.yeonjae.ovlo.application.dto.result.*;
import java.util.List;
public interface UniversityApplicationUseCase {
    MemberResult register(UniversityRegistrationCommand command);
    UniversityApplicationResult apply(long memberId, long schoolId, String department, String email, String message);
    List<UniversityApplicationResult> mine(long memberId);
    List<UniversityApplicationResult> list(CalendarActor actor, String status, int page);
    void review(CalendarActor actor, long id, String action, String note, long version);
}
