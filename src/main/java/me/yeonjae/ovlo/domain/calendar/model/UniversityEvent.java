package me.yeonjae.ovlo.domain.calendar.model;

import me.yeonjae.ovlo.domain.calendar.exception.CalendarStateException;

public final class UniversityEvent {
    private final Long id;
    private final long universityId;
    private EventDetails details;
    private final EventOrigin origin;
    private EventSource source;
    private EventStatus status;
    private final long createdBy;
    private long updatedBy;
    private final long version;

    private UniversityEvent(Long id, long universityId, EventDetails details, EventOrigin origin,
                            EventSource source, EventStatus status, long createdBy, long updatedBy, long version) {
        if (universityId <= 0 || details == null || origin == null || status == null) throw new IllegalArgumentException("잘못된 학교 일정입니다");
        requireSource(origin, source);
        this.id = id; this.universityId = universityId; this.details = details; this.origin = origin;
        this.source = source; this.status = status; this.createdBy = createdBy; this.updatedBy = updatedBy; this.version = version;
    }
    public static UniversityEvent create(long universityId, EventDetails details, EventOrigin origin, EventSource source, long actor) {
        return new UniversityEvent(null, universityId, details, origin, source, EventStatus.DRAFT, actor, actor, 0);
    }
    public static UniversityEvent restore(Long id, long universityId, EventDetails details, EventOrigin origin,
                                          EventSource source, EventStatus status, long createdBy, long updatedBy, long version) {
        return new UniversityEvent(id, universityId, details, origin, source, status, createdBy, updatedBy, version);
    }
    public void edit(EventDetails details, EventSource source, long actor) {
        if (status == EventStatus.CANCELLED) throw new CalendarStateException("취소된 일정은 변경할 수 없습니다");
        if (details == null) throw new IllegalArgumentException("일정 내용이 필요합니다");
        requireSource(origin, source);
        this.details = details; this.source = source; this.updatedBy = actor;
    }
    public void publish() {
        if (status != EventStatus.DRAFT) throw new CalendarStateException("초안만 게시할 수 있습니다");
        status = EventStatus.PUBLISHED;
    }
    public void cancel() {
        if (status != EventStatus.PUBLISHED) throw new CalendarStateException("게시된 일정만 취소할 수 있습니다");
        status = EventStatus.CANCELLED;
    }
    public void recordActor(long actor) { this.updatedBy = actor; }
    private static void requireSource(EventOrigin origin, EventSource source) {
        if (origin == EventOrigin.OVLO_CURATED && source == null) throw new IllegalArgumentException("대리 등록에는 공식 출처와 확인일이 필요합니다");
    }
    public Long id() { return id; }
    public long universityId() { return universityId; }
    public EventDetails details() { return details; }
    public EventOrigin origin() { return origin; }
    public EventSource source() { return source; }
    public EventStatus status() { return status; }
    public long createdBy() { return createdBy; }
    public long updatedBy() { return updatedBy; }
    public long version() { return version; }
}
