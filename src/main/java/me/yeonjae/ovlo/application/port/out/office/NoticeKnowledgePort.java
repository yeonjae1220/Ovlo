package me.yeonjae.ovlo.application.port.out.office;

import me.yeonjae.ovlo.domain.office.model.*;
import java.time.Instant;
import java.util.List;

public interface NoticeKnowledgePort {
    record Scope(Long id, Long officeId, String department, String url, boolean enabled, long revision) {}
    record Audit(Long id, Long officeId, Long noticeId, int version, long actorId, String action,
                 String evidence, String snapshot, Instant createdAt) {}
    void lockOffice(Long officeId);
    List<Scope> scopes(Long officeId);
    Scope saveScope(Scope scope);
    void audit(Notice notice, long actorId, String action, String evidence, Instant now);
    List<Audit> history(Long officeId, Long noticeId);
    void saveCitations(Long inquiryId, List<NoticeCitation> citations);
    List<NoticeCitation> citations(Long inquiryId);
    List<Long> reviewDueIds(java.time.LocalDate until);
    boolean reminderSent(Long noticeId, java.time.LocalDate due, boolean overdue);
    void markReminder(Long noticeId, java.time.LocalDate due, boolean overdue);
}
