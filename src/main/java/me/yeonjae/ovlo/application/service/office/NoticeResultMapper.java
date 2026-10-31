package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.dto.result.NoticeResult;
import me.yeonjae.ovlo.application.dto.result.NoticeSummaryResult;
import me.yeonjae.ovlo.application.dto.result.NoticeTranslationResult;
import me.yeonjae.ovlo.application.dto.result.StudentNoticeResult;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.NoticeTranslation;
import me.yeonjae.ovlo.domain.office.model.TranslationStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Notice → 결과 DTO. 순수 함수 모음(상태 없음). */
public final class NoticeResultMapper {

    private NoticeResultMapper() {}

    public static NoticeResult toResult(Notice n, LocalDate today) {
        List<NoticeTranslationResult> translations = n.getTranslations().stream()
                .map(t -> new NoticeTranslationResult(t.getLang(), t.getTitle(), t.getBody(), t.getStatus(),
                        t.isStaleFor(n.getVersion()), t.getGeneratedAt()))
                .toList();
        return new NoticeResult(n.getId(), n.getOfficeId(), n.getCategory(), n.getTitle(), n.getBody(), n.getSourceLang(),
                n.getSourceUrl(), n.getAttachmentMediaIds(), n.getTerm(), n.getEffectiveFrom(), n.getExpiresAt(),
                n.getStatus(), n.getApprovedBy(), n.getApprovedAt(), n.getVersion(), n.isActiveOn(today),
                n.getCreatedAt(), n.getUpdatedAt(), translations, n.translationsToGenerate(NoticeLang.ALL));
    }

    public static NoticeSummaryResult toSummary(Notice n) {
        int missing = n.translationsToGenerate(NoticeLang.ALL).size();
        int translated = NoticeLang.ALL.size() - 1 - missing; // 원문 언어 제외
        return new NoticeSummaryResult(n.getId(), n.getCategory(), n.getTitle(), n.getTerm(), n.getEffectiveFrom(),
                n.getExpiresAt(), n.getStatus(), n.getVersion(), translated, missing, n.getUpdatedAt());
    }

    /** 요청 언어의 최신 번역이 있으면 그것을, 없으면 원문을 돌려준다. */
    public static StudentNoticeResult toStudent(Notice n, NoticeLang requested) {
        Optional<NoticeTranslation> fresh = n.getTranslation(requested).filter(t -> !t.isStaleFor(n.getVersion()));
        if (requested != n.getSourceLang() && fresh.isPresent()) {
            NoticeTranslation t = fresh.get();
            return new StudentNoticeResult(n.getId(), n.getCategory(), t.getTitle(), t.getBody(), t.getLang(),
                    t.getStatus() == TranslationStatus.MACHINE, n.getSourceUrl(), n.getAttachmentMediaIds(), n.getTerm(),
                    n.getEffectiveFrom(), n.getExpiresAt());
        }
        return new StudentNoticeResult(n.getId(), n.getCategory(), n.getTitle(), n.getBody(), n.getSourceLang(), false,
                n.getSourceUrl(), n.getAttachmentMediaIds(), n.getTerm(), n.getEffectiveFrom(), n.getExpiresAt());
    }
}
