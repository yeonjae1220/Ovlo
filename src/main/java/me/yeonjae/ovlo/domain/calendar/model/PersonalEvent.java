package me.yeonjae.ovlo.domain.calendar.model;

public final class PersonalEvent {
    private final Long id;
    private final long memberId;
    private EventDetails details;
    private final long version;
    private PersonalEvent(Long id, long memberId, EventDetails details, long version) {
        if (memberId <= 0 || details == null) throw new IllegalArgumentException("잘못된 개인 일정입니다");
        this.id = id; this.memberId = memberId; this.details = details; this.version = version;
    }
    public static PersonalEvent create(long memberId, EventDetails details) { return new PersonalEvent(null, memberId, details, 0); }
    public static PersonalEvent restore(Long id, long memberId, EventDetails details, long version) { return new PersonalEvent(id, memberId, details, version); }
    public void edit(EventDetails details) {
        if (details == null) throw new IllegalArgumentException("일정 내용이 필요합니다");
        this.details = details;
    }
    public Long id() { return id; }
    public long memberId() { return memberId; }
    public EventDetails details() { return details; }
    public long version() { return version; }
}
