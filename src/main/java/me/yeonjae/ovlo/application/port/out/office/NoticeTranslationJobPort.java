package me.yeonjae.ovlo.application.port.out.office;

import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Durable per-version, per-language work. All completions are fenced by an expiring owner token. */
public interface NoticeTranslationJobPort {
    record Claim(long noticeId, int version, NoticeLang lang, UUID owner) {}
    void enqueue(long noticeId, int version, List<NoticeLang> languages);
    void activateApproved(long noticeId, int version);
    Optional<Claim> claim(long noticeId, int version, NoticeLang lang);
    boolean isHeld(Claim claim);
    boolean beginAttempt(Claim claim);
    void finish(Claim claim, boolean rejected);
    void reopenRejected(long noticeId, int version);
    void resumePaused(long noticeId, int version);
    void defer(Claim claim, long delaySeconds);
    List<Long> dueNotices();
}
