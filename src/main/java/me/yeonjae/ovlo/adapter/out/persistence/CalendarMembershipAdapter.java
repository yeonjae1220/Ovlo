package me.yeonjae.ovlo.adapter.out.persistence;
import me.yeonjae.ovlo.adapter.out.persistence.entity.*;
import me.yeonjae.ovlo.adapter.out.persistence.repository.*;
import me.yeonjae.ovlo.application.port.out.calendar.CalendarMembershipPort;
import me.yeonjae.ovlo.domain.calendar.model.*;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;
import java.util.*;

@Component
public class CalendarMembershipAdapter implements CalendarMembershipPort {
    private final CalendarSubscriptionJpaRepository subscriptions;
    private final UniversityCalendarManagerJpaRepository managers;
    private final UniversityApplicationJpaRepository applications;
    private final CalendarAuditJpaRepository audits;
    public CalendarMembershipAdapter(CalendarSubscriptionJpaRepository subscriptions, UniversityCalendarManagerJpaRepository managers,
                                     UniversityApplicationJpaRepository applications, CalendarAuditJpaRepository audits) {
        this.subscriptions = subscriptions; this.managers = managers; this.applications = applications; this.audits = audits;
    }
    public List<Long> subscriptions(long memberId) { return subscriptions.findByMemberId(memberId).stream().map(CalendarSubscriptionJpaEntity::getUniversityId).toList(); }
    public void subscribe(long memberId, long schoolId) { subscriptions.save(new CalendarSubscriptionJpaEntity(memberId, schoolId)); }
    public void unsubscribe(long memberId, long schoolId) { subscriptions.deleteById(new CalendarMembershipKey(memberId, schoolId)); }
    public List<Long> managedSchools(long memberId) { return managers.findByMemberId(memberId).stream().map(UniversityCalendarManagerJpaEntity::getUniversityId).toList(); }
    public boolean manages(long memberId, long schoolId) { return managers.existsById(new CalendarMembershipKey(memberId, schoolId)); }
    public void grant(long memberId, long schoolId) { managers.save(new UniversityCalendarManagerJpaEntity(memberId, schoolId)); }
    public void revoke(long memberId, long schoolId) { managers.deleteById(new CalendarMembershipKey(memberId, schoolId)); }
    public Optional<UniversityApplication> findApplication(long id) { return applications.findById(id).map(UniversityApplicationJpaEntity::domain); }
    public boolean hasOpenApplication(long memberId, long schoolId) { return applications.existsByMemberIdAndUniversityIdAndActiveSlotTrue(memberId, schoolId); }
    public List<UniversityApplication> applications(Long memberId, ApplicationStatus status, int page, int size) {
        return applications.search(memberId, status, PageRequest.of(page, size)).stream().map(UniversityApplicationJpaEntity::domain).toList();
    }
    public UniversityApplication saveApplication(UniversityApplication a) {
        var row = a.id() == null ? new UniversityApplicationJpaEntity() : applications.findById(a.id()).orElseThrow();
        if (a.id() != null) CalendarPersistenceAdapter.checkVersion(row.getVersion(), a.version(), a.id());
        row.setMemberId(a.memberId()); row.setUniversityId(a.universityId()); row.setDepartment(a.department()); row.setContactEmail(a.contactEmail());
        row.setMessage(a.message()); row.setStatus(a.status()); row.setActiveSlot(a.status() == ApplicationStatus.PENDING || a.status() == ApplicationStatus.APPROVED ? true : null);
        row.setReviewNote(a.reviewNote()); row.setReviewedBy(a.reviewedBy()); row.setCreatedAt(a.createdAt()); row.setReviewedAt(a.reviewedAt());
        var saved = applications.saveAndFlush(row).domain();
        var audit = new CalendarAuditJpaEntity(); audit.setAggregateType("UNIVERSITY_APPLICATION"); audit.setAggregateId(saved.id());
        audit.setActor(a.reviewedBy() == null ? "member:" + a.memberId() : a.reviewedBy()); audit.setAction(a.status().name());
        audit.setSnapshot("school=" + a.universityId() + "; member=" + a.memberId() + "; note=" + a.reviewNote()); audit.setCreatedAt(Instant.now()); audits.save(audit);
        return saved;
    }
}
