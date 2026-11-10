package me.yeonjae.ovlo.application.dto.command;

/** Session administrators are constructed only by the /admin adapter after its security chain. */
public record CalendarActor(Long memberId, String administrator) {
    public static CalendarActor member(Long id) { return new CalendarActor(id, null); }
    public static CalendarActor administrator(String name) { return new CalendarActor(null, name); }
    public long auditId() { return memberId == null ? 0 : memberId; }
    public String auditName() { return administrator != null ? "admin:" + administrator : "member:" + memberId; }
}
