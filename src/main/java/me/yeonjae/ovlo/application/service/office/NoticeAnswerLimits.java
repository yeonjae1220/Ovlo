package me.yeonjae.ovlo.application.service.office;

/** Saved notice body and answer context both use Java UTF-16 character counts. */
public final class NoticeAnswerLimits {
    public static final int MAX_BODY_CHARS = 3_000;

    private NoticeAnswerLimits() { }
}
