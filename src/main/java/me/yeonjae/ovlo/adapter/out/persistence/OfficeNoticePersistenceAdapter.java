package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.entity.OfficeNoticeJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.entity.OfficeNoticeTranslationJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.repository.OfficeNoticeJpaRepository;
import me.yeonjae.ovlo.application.port.out.office.LoadNoticePort;
import me.yeonjae.ovlo.application.port.out.office.SaveNoticePort;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.NoticeStatus;
import me.yeonjae.ovlo.domain.office.model.NoticeTranslation;
import me.yeonjae.ovlo.domain.office.model.TranslationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Notice 애그리거트 영속. 번역 행은 (notice_id, lang) 으로 식별해 덮어쓴다. */
@Component
public class OfficeNoticePersistenceAdapter implements LoadNoticePort, SaveNoticePort {

    private final OfficeNoticeJpaRepository repository;

    public OfficeNoticePersistenceAdapter(OfficeNoticeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Notice> findById(Long noticeId) {
        return repository.findWithTranslationsById(noticeId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Notice> findByOffice(Long officeId, NoticeStatus status, NoticeCategory category, Pageable pageable) {
        return repository.findByOfficeFiltered(officeId,
                status != null ? status.name() : null,
                category != null ? category.name() : null,
                pageable).map(this::toDomain);
    }

    @Override
    public List<Notice> findActiveForStudent(Long officeId, NoticeCategory category, LocalDate today) {
        return repository.findActiveForStudent(officeId, category != null ? category.name() : null, today).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Long> findIdsToExpire(LocalDate today) {
        return repository.findIdsToExpire(today);
    }

    @Override
    public List<Long> findIdsExpiringWithin(LocalDate today, int daysAhead) {
        return repository.findIdsExpiringBetween(today, today.plusDays(daysAhead));
    }

    @Override
    @Transactional
    public Notice save(Notice notice) {
        OfficeNoticeJpaEntity e = notice.getId() == null
                ? new OfficeNoticeJpaEntity()
                : repository.findWithTranslationsById(notice.getId())
                        .orElseThrow(() -> new IllegalStateException("저장할 공지가 없습니다: id=" + notice.getId()));
        e.setOfficeId(notice.getOfficeId());
        e.setCategory(notice.getCategory().name());
        e.setTitle(notice.getTitle());
        e.setBody(notice.getBody());
        e.setSourceLang(notice.getSourceLang().code());
        e.setSourceUrl(notice.getSourceUrl());
        e.setAttachmentMediaIds(joinIds(notice.getAttachmentMediaIds()));
        e.setTerm(notice.getTerm());
        e.setEffectiveFrom(notice.getEffectiveFrom());
        e.setExpiresAt(notice.getExpiresAt());
        e.setStatus(notice.getStatus().name());
        e.setApprovedBy(notice.getApprovedBy());
        e.setApprovedAt(notice.getApprovedAt());
        e.setExpiryWarnedAt(notice.getExpiryWarnedAt());
        e.setVersion(notice.getVersion());
        e.setCreatedBy(notice.getCreatedBy());
        if (e.getId() == null) e.setCreatedAt(notice.getCreatedAt());

        Map<String, OfficeNoticeTranslationJpaEntity> byLang = e.getTranslations().stream()
                .collect(Collectors.toMap(OfficeNoticeTranslationJpaEntity::getLang, Function.identity()));
        for (NoticeTranslation t : notice.getTranslations()) {
            OfficeNoticeTranslationJpaEntity row = byLang.get(t.getLang().code());
            if (row == null) {
                row = new OfficeNoticeTranslationJpaEntity();
                row.setLang(t.getLang().code());
                e.addTranslation(row);
            }
            row.setTitle(t.getTitle());
            row.setBody(t.getBody());
            row.setStatus(t.getStatus().name());
            row.setSourceVersion(t.getSourceVersion());
            row.setGeneratedAt(t.getGeneratedAt());
        }
        OfficeNoticeJpaEntity saved = repository.saveAndFlush(e);
        // 호출자가 넘긴 객체를 계속 쓰더라도 두 번째 save 가 새 행을 만들지 않도록 id 를 돌려준다
        if (notice.getId() == null) notice.assignId(saved.getId());
        return toDomain(saved);
    }

    private Notice toDomain(OfficeNoticeJpaEntity e) {
        List<NoticeTranslation> translations = e.getTranslations().stream()
                .map(t -> NoticeTranslation.restore(NoticeLang.fromCode(t.getLang()).orElseThrow(), t.getTitle(), t.getBody(),
                        TranslationStatus.valueOf(t.getStatus()), t.getSourceVersion(), t.getGeneratedAt()))
                .toList();
        return Notice.restore(e.getId(), e.getOfficeId(), NoticeCategory.valueOf(e.getCategory()), e.getTitle(), e.getBody(),
                NoticeLang.fromCode(e.getSourceLang()).orElse(NoticeLang.KO), e.getSourceUrl(), splitIds(e.getAttachmentMediaIds()),
                e.getTerm(), e.getEffectiveFrom(), e.getExpiresAt(), NoticeStatus.valueOf(e.getStatus()), e.getApprovedBy(),
                e.getApprovedAt(), e.getExpiryWarnedAt(), e.getVersion(), e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt(),
                translations);
    }

    private static String joinIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static List<Long> splitIds(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).toList();
    }
}
