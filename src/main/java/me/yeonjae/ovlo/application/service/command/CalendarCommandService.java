package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.*;
import me.yeonjae.ovlo.application.dto.result.CalendarEventResult;
import me.yeonjae.ovlo.application.port.in.calendar.CalendarUseCase;
import me.yeonjae.ovlo.application.port.out.calendar.*;
import me.yeonjae.ovlo.domain.calendar.model.*;
import me.yeonjae.ovlo.domain.calendar.exception.CalendarException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static me.yeonjae.ovlo.domain.calendar.exception.CalendarException.ErrorType.*;

@Service
@Transactional
public class CalendarCommandService implements CalendarUseCase {
    private final CalendarEventPort events;
    private final CalendarMembershipPort memberships;
    private final CalendarAccessService access;
    public CalendarCommandService(CalendarEventPort events, CalendarMembershipPort memberships, CalendarAccessService access) {
        this.events = events; this.memberships = memberships; this.access = access;
    }
    @Override public CalendarEventResult createPersonal(long memberId, SaveCalendarEventCommand c) {
        access.active(memberId);
        return CalendarEventResult.personal(events.savePersonalEvent(PersonalEvent.create(memberId, details(c))));
    }
    @Override public CalendarEventResult updatePersonal(long memberId, long id, SaveCalendarEventCommand c) {
        access.active(memberId);
        var e = personal(id, memberId); CalendarAccessService.version(e.version(), c.version()); e.edit(details(c));
        return CalendarEventResult.personal(events.savePersonalEvent(e));
    }
    @Override public void deletePersonal(long memberId, long id, long version) {
        access.active(memberId); var e = personal(id, memberId); CalendarAccessService.version(e.version(), version);
        events.deletePersonalEvent(e);
    }
    @Override public CalendarEventResult createSchool(CalendarActor actor, long schoolId, SaveCalendarEventCommand c) {
        access.manage(actor, schoolId);
        var origin = access.isAdmin(actor) ? EventOrigin.OVLO_CURATED : EventOrigin.UNIVERSITY;
        var event = UniversityEvent.create(schoolId, details(c), origin, source(c), actor.auditId());
        return result(events.saveSchoolEvent(event, actor.auditName()));
    }
    @Override public CalendarEventResult updateSchool(CalendarActor actor, long id, SaveCalendarEventCommand c) {
        var e = school(id); access.manage(actor, e.universityId()); CalendarAccessService.version(e.version(), c.version());
        e.edit(details(c), source(c), actor.auditId());
        return result(events.saveSchoolEvent(e, actor.auditName()));
    }
    @Override public CalendarEventResult changeStatus(CalendarActor actor, long id, String action, long version) {
        var e = school(id); access.manage(actor, e.universityId()); CalendarAccessService.version(e.version(), version);
        if ("publish".equals(action)) e.publish();
        else if ("cancel".equals(action)) e.cancel();
        else throw new IllegalArgumentException("지원하지 않는 상태 변경입니다");
        e.recordActor(actor.auditId());
        return result(events.saveSchoolEvent(e, actor.auditName()));
    }
    @Override public void deleteDraft(CalendarActor actor, long id, long version) {
        var e = school(id); access.manage(actor, e.universityId()); CalendarAccessService.version(e.version(), version);
        if (e.status() != EventStatus.DRAFT) throw new CalendarException("초안만 삭제할 수 있습니다. 게시된 일정은 취소해 주세요", BAD_REQUEST);
        events.deleteSchoolDraft(e, actor.auditName());
    }
    @Override public void subscribe(long memberId, long schoolId, boolean subscribed) {
        access.active(memberId); access.school(schoolId);
        if (!subscribed) { memberships.unsubscribe(memberId, schoolId); return; }
        var ids = memberships.subscriptions(memberId);
        if (ids.contains(schoolId)) return;
        if (ids.size() >= 20) throw new CalendarException("학교는 최대 20개까지 구독할 수 있습니다", BAD_REQUEST);
        memberships.subscribe(memberId, schoolId);
    }
    private UniversityEvent school(long id) { return events.findSchoolEvent(id).orElseThrow(() -> new CalendarException("일정을 찾을 수 없습니다", NOT_FOUND)); }
    private PersonalEvent personal(long id, long memberId) { return events.findPersonalEvent(id, memberId).orElseThrow(() -> new CalendarException("일정을 찾을 수 없습니다", NOT_FOUND)); }
    private CalendarEventResult result(UniversityEvent e) { return CalendarEventResult.school(e, access.school(e.universityId()).getName()); }
    private EventSource source(SaveCalendarEventCommand c) {
        if ((c.sourceUrl() == null || c.sourceUrl().isBlank()) && c.sourceCheckedOn() == null) return null;
        return new EventSource(c.sourceUrl(), c.sourceCheckedOn());
    }
    private EventDetails details(SaveCalendarEventCommand c) {
        return new EventDetails(c.title(), c.description(), EventCategory.valueOf(c.category()),
                new EventPeriod(c.allDay(), c.startDate(), c.endDate(), c.startsAt(), c.endsAt(), c.timeZone()),
                c.location(), c.actionUrl(), c.deadlineAt(), c.audience());
    }
}
