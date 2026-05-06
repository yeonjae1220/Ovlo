package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.entity.UniversityReportJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.entity.UniversityReportTranslationJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.repository.UniversityReportJpaRepository;
import me.yeonjae.ovlo.adapter.out.persistence.repository.UniversityReportTranslationJpaRepository;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.UniversityReportResult;
import me.yeonjae.ovlo.application.dto.result.UniversityReportSummaryResult;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityReportPort;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class UniversityReportPersistenceAdapter implements LoadUniversityReportPort {

    private final UniversityReportJpaRepository reportRepo;
    private final UniversityReportTranslationJpaRepository translationRepo;

    public UniversityReportPersistenceAdapter(
            UniversityReportJpaRepository reportRepo,
            UniversityReportTranslationJpaRepository translationRepo) {
        this.reportRepo = reportRepo;
        this.translationRepo = translationRepo;
    }

    @Override
    public PageResult<UniversityReportSummaryResult> findPageByLang(String lang, String keyword, int page, int size) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        int offset = page * size;

        List<UniversityReportJpaEntity> entities = reportRepo.findPublishedPage(kw, size, offset);
        long total = reportRepo.countPublished(kw);

        List<Long> ids = entities.stream().map(UniversityReportJpaEntity::getId).toList();
        Map<Long, List<UniversityReportTranslationJpaEntity>> translationsByReport =
                ids.isEmpty() ? Collections.emptyMap() :
                translationRepo.findByReportIds(ids).stream()
                        .collect(Collectors.groupingBy(t -> t.getId().getReportId()));

        List<UniversityReportSummaryResult> content = entities.stream()
                .map(r -> {
                    String[] supported = r.getSupportedLangs();
                    List<UniversityReportTranslationJpaEntity> ts =
                            translationsByReport.getOrDefault(r.getId(), Collections.emptyList());

                    String effectiveLang = lang;
                    if (supported != null && supported.length > 0 && !Arrays.asList(supported).contains(lang)) {
                        effectiveLang = supported[0];
                    }
                    final String resolvedLang = effectiveLang;
                    Optional<UniversityReportTranslationJpaEntity> t =
                            ts.stream().filter(x -> resolvedLang.equals(x.getId().getLang())).findFirst();

                    String title   = t.map(UniversityReportTranslationJpaEntity::getTitle).orElse("");
                    String summary = t.map(UniversityReportTranslationJpaEntity::getSummary).orElse(null);
                    List<String> langs = supported != null ? Arrays.asList(supported) : Collections.emptyList();
                    return new UniversityReportSummaryResult(
                            r.getId(), title, summary,
                            r.getSourceVideoCount(), r.getSourceWebCount(), langs,
                            r.getCreatedAt());
                })
                .toList();

        return PageResult.of(content, total, page, size);
    }

    @Override
    public Optional<UniversityReportResult> findByIdAndLang(Long id, String lang) {
        return reportRepo.findById(id)
                .flatMap(r -> resolveTranslation(r, lang));
    }

    @Override
    public Optional<UniversityReportResult> findByGlobalUnivIdAndLang(Long globalUnivId, String lang) {
        return reportRepo.findByGlobalUnivId(globalUnivId)
                .flatMap(r -> resolveTranslation(r, lang));
    }

    @Override
    public Optional<UniversityReportResult> findByExchangeUnivIdAndLang(Long exchangeUnivId, String lang) {
        return reportRepo.findByExchangeUnivId(exchangeUnivId)
                .flatMap(r -> resolveTranslation(r, lang));
    }

    @Override
    public List<String> findLangsByReportId(Long reportId) {
        return translationRepo.findLangsByReportId(reportId);
    }

    private Optional<UniversityReportResult> resolveTranslation(UniversityReportJpaEntity r, String lang) {
        String effectiveLang = lang;
        String[] supported = r.getSupportedLangs();
        if (supported != null && supported.length > 0) {
            boolean hasLang = Arrays.asList(supported).contains(lang);
            if (!hasLang) effectiveLang = supported[0];
        }
        return translationRepo.findByIdReportIdAndIdLang(r.getId(), effectiveLang)
                .map(t -> new UniversityReportResult(
                        r.getId(), r.getGlobalUnivId(), t.getLang(),
                        t.getTitle(), t.getSummary(), t.getBody(), t.getContent(),
                        r.getSourceVideoCount(), r.getSourceWebCount()));
    }
}
