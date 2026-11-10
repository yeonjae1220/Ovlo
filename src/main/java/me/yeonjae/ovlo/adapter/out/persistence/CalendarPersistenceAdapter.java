package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.entity.*;
import me.yeonjae.ovlo.adapter.out.persistence.repository.*;
import me.yeonjae.ovlo.application.port.out.calendar.CalendarEventPort;
import me.yeonjae.ovlo.domain.calendar.model.*;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import java.time.*;
import java.util.*;

@Component
public class CalendarPersistenceAdapter implements CalendarEventPort {
    private final UniversityEventJpaRepository schools;
    private final PersonalEventJpaRepository personal;
    private final CalendarAuditJpaRepository audits;
    public CalendarPersistenceAdapter(UniversityEventJpaRepository schools, PersonalEventJpaRepository personal, CalendarAuditJpaRepository audits) {
        this.schools = schools; this.personal = personal; this.audits = audits;
    }
    public Optional<UniversityEvent> findSchoolEvent(long id) { return schools.findById(id).map(UniversityEventJpaEntity::domain); }
    public Optional<PersonalEvent> findPersonalEvent(long id, long memberId) { return personal.findByIdAndMemberId(id, memberId).map(PersonalEventJpaEntity::domain); }
    public UniversityEvent saveSchoolEvent(UniversityEvent e, String actor) {
        var row = e.id() == null ? new UniversityEventJpaEntity() : schools.findById(e.id()).orElseThrow();
        if (e.id() != null) checkVersion(row.getVersion(), e.version(), e.id());
        row.apply(e.details()); row.setUniversityId(e.universityId()); row.setOrigin(e.origin()); row.setStatus(e.status());
        row.setSourceUrl(e.source() == null ? null : e.source().url()); row.setSourceCheckedOn(e.source() == null ? null : e.source().checkedOn());
        row.setCreatedBy(e.createdBy()); row.setUpdatedBy(e.updatedBy());
        var saved = schools.saveAndFlush(row).domain();
        audit(saved.id(), actor, saved.status().name(), snapshot(saved));
        return saved;
    }
    public PersonalEvent savePersonalEvent(PersonalEvent e) {
        var row = e.id() == null ? new PersonalEventJpaEntity() : personal.findByIdAndMemberId(e.id(), e.memberId()).orElseThrow();
        if (e.id() != null) checkVersion(row.getVersion(), e.version(), e.id());
        row.apply(e.details()); row.setMemberId(e.memberId());
        return personal.saveAndFlush(row).domain();
    }
    public void deletePersonalEvent(PersonalEvent e) {
        var row = personal.findByIdAndMemberId(e.id(), e.memberId()).orElseThrow(); checkVersion(row.getVersion(), e.version(), e.id());
        personal.delete(row); personal.flush();
    }
    public void deleteSchoolDraft(UniversityEvent e, String actor) {
        var row = schools.findById(e.id()).orElseThrow(); checkVersion(row.getVersion(), e.version(), e.id());
        audit(e.id(), actor, "DELETE_DRAFT", snapshot(e)); schools.delete(row); schools.flush();
    }
    public List<UniversityEvent> schoolEvents(List<Long> ids, LocalDate from, LocalDate to, Instant start, Instant end, boolean drafts, int limit) {
        return schools.overlapping(ids, from, to, start, end, drafts, PageRequest.of(0, limit)).stream().map(UniversityEventJpaEntity::domain).toList();
    }
    public List<PersonalEvent> personalEvents(long memberId, LocalDate from, LocalDate to, Instant start, Instant end, int limit) {
        return personal.overlapping(memberId, from, to, start, end, PageRequest.of(0, limit)).stream().map(PersonalEventJpaEntity::domain).toList();
    }
    static void checkVersion(Long actual, long expected, Long id) {
        if (actual == null || actual != expected) throw new ObjectOptimisticLockingFailureException("Calendar", id);
    }
    private String snapshot(UniversityEvent e) {
        return "school=" + e.universityId() + "; origin=" + e.origin() + "; creator=" + e.createdBy() + "; version=" + e.version()
                + "; source=" + e.source() + "; details=" + e.details();
    }
    private void audit(Long id, String actor, String action, String snapshot) {
        var a = new CalendarAuditJpaEntity(); a.setAggregateType("UNIVERSITY_EVENT"); a.setAggregateId(id);
        a.setActor(actor); a.setAction(action); a.setSnapshot(snapshot); a.setCreatedAt(Instant.now()); audits.save(a);
    }
}
