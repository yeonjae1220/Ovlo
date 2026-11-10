package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.command.CalendarActor;
import me.yeonjae.ovlo.application.dto.result.*;
import me.yeonjae.ovlo.application.port.in.calendar.CalendarQuery;
import me.yeonjae.ovlo.application.port.out.calendar.*;
import me.yeonjae.ovlo.application.service.command.CalendarAccessService;
import me.yeonjae.ovlo.domain.calendar.exception.CalendarException;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static me.yeonjae.ovlo.domain.calendar.exception.CalendarException.ErrorType.BAD_REQUEST;

@Service
@Transactional(readOnly = true)
public class CalendarQueryService implements CalendarQuery {
    private final CalendarEventPort events;
    private final CalendarMembershipPort memberships;
    private final CalendarAccessService access;
    public CalendarQueryService(CalendarEventPort events, CalendarMembershipPort memberships, CalendarAccessService access) {
        this.events = events; this.memberships = memberships; this.access = access;
    }
    @Override public CalendarOptionsResult options(long memberId) {
        var member = access.active(memberId);
        Set<Long> suggestions = new LinkedHashSet<>();
        if (member.getHomeUniversityId() != null) suggestions.add(member.getHomeUniversityId().value());
        member.getUniversityExperiences().forEach(e -> suggestions.add(e.universityId().value()));
        return new CalendarOptionsResult(member.getRole().name(), schools(memberships.subscriptions(memberId)), schools(new ArrayList<>(suggestions)),
                member.getRole() == MemberRole.UNIV || member.getRole() == MemberRole.ADMIN ? schools(memberships.managedSchools(memberId)) : List.of());
    }
    @Override public List<CalendarEventResult> calendar(long memberId, LocalDate from, LocalDate to, String timeZone) {
        access.active(memberId); var zone = validate(from, to, timeZone);
        var start = from.atStartOfDay(zone).toInstant(); var end = to.atStartOfDay(zone).toInstant();
        List<CalendarEventResult> result = new ArrayList<>();
        var ids = memberships.subscriptions(memberId);
        Map<Long,String> names = new HashMap<>(); schools(ids).forEach(s -> names.put(s.id(), s.name()));
        if (!ids.isEmpty()) events.schoolEvents(ids, from, to, start, end, false, 501)
                .forEach(e -> result.add(CalendarEventResult.school(e, names.get(e.universityId()))));
        events.personalEvents(memberId, from, to, start, end, 501).forEach(e -> result.add(CalendarEventResult.personal(e)));
        return bounded(result);
    }
    @Override public List<CalendarEventResult> management(CalendarActor actor, long schoolId, LocalDate from, LocalDate to, String timeZone) {
        access.manage(actor, schoolId); var zone = validate(from, to, timeZone);
        String name = access.school(schoolId).getName();
        return bounded(events.schoolEvents(List.of(schoolId), from, to, from.atStartOfDay(zone).toInstant(), to.atStartOfDay(zone).toInstant(), true, 501)
                .stream().map(e -> CalendarEventResult.school(e, name)).toList());
    }
    private List<CalendarOptionsResult.School> schools(List<Long> ids) {
        return ids.stream().distinct().limit(100).map(id -> new CalendarOptionsResult.School(id, access.school(id).getName())).toList();
    }
    private ZoneId validate(LocalDate from, LocalDate to, String zone) {
        if (from == null || to == null || !to.isAfter(from) || ChronoUnit.DAYS.between(from, to) > 93)
            throw new CalendarException("조회 기간은 1~93일이어야 합니다", BAD_REQUEST);
        try { return ZoneId.of(zone); } catch (Exception ex) { throw new IllegalArgumentException("올바른 시간대를 입력해 주세요"); }
    }
    private List<CalendarEventResult> bounded(List<CalendarEventResult> result) {
        if (result.size() > 500) throw new CalendarException("일정이 많습니다. 조회 기간을 줄여 주세요", BAD_REQUEST);
        return result;
    }
}
