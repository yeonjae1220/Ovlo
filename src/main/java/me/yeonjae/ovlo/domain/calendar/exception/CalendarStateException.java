package me.yeonjae.ovlo.domain.calendar.exception;
/** An invalid calendar lifecycle transition is a client conflict, not a server failure. */
public class CalendarStateException extends IllegalStateException {
    public CalendarStateException(String message) { super(message); }
}
