package me.yeonjae.ovlo.application.service.office;

/** Diagnostic only: no notice content, member ID or office ID leaves the write transaction. */
public record LongNoticeSavedEvent(Action action) {
    public enum Action { REGISTER, UPDATE }
}
