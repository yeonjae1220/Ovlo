package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/** Select all eligible documents only when the complete corpus fits the existing prompt budget. */
public final class SmallNoticeCorpus {
    public static final int MAX_CONTEXT_BYTES = 30_000;
    private SmallNoticeCorpus() {}

    public static List<Notice> eligible(List<Notice> notices, Long officeId, NoticeLang lang, LocalDate today,
                                        NoticeGovernanceService governance) {
        return notices.stream()
                .filter(n -> officeId.equals(n.getOfficeId()))
                .filter(n -> n.isGroundingEligible(today, lang))
                .filter(n -> governance.eligible(n, today))
                .toList();
    }

    /** Present empty list means no eligible corpus; empty Optional means use normal ranked retrieval.
     * Count the full requested-language title/body and public applicability before the legacy 200/3000-character truncation.
     * Thus no additional truncation is needed to fit the byte budget on this path.
     */
    public static Optional<List<Notice>> fitting(List<Notice> eligible, NoticeLang lang, int candidateLimit) {
        if (candidateLimit <= 0 || eligible.size() > candidateLimit) return Optional.empty();
        int remaining = MAX_CONTEXT_BYTES;
        for (Notice notice : eligible) {
            var translation = notice.getTranslation(lang).filter(t -> !t.isStaleFor(notice.getVersion()));
            String title = translation.map(t -> t.getTitle()).orElse(notice.getTitle());
            String body = translation.map(t -> t.getBody()).orElse(notice.getBody());
            var source = notice.getSource();
            String metadata = Objects.toString(notice.getSourceUrl(), "")
                    + (source == null ? "" : source.audience() + source.campus());
            remaining -= (title + body + metadata).getBytes(StandardCharsets.UTF_8).length;
            if (remaining < 0) return Optional.empty();
        }
        return Optional.of(eligible.stream().sorted(Comparator.comparing(Notice::getId).reversed()).toList());
    }
}
