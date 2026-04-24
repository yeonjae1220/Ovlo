package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.entity.ExchangeUniversityJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.entity.ExchangeVideoReviewJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.repository.ExchangeUniversityJpaRepository;
import me.yeonjae.ovlo.adapter.out.persistence.repository.ExchangeVideoReviewJpaRepository;
import me.yeonjae.ovlo.application.dto.result.ExchangeUniversityCountryResult;
import me.yeonjae.ovlo.application.port.out.university.LoadExchangeUniversityPort;
import me.yeonjae.ovlo.domain.university.model.ExchangeUniversity;
import me.yeonjae.ovlo.domain.university.model.ExchangeUniversityId;
import me.yeonjae.ovlo.domain.university.model.VideoReview;
import me.yeonjae.ovlo.domain.university.model.VideoReviewId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import static java.util.stream.Collectors.toMap;

@Component
public class ExchangeUniversityPersistenceAdapter implements LoadExchangeUniversityPort {

    private final ExchangeUniversityJpaRepository exchangeUniversityRepo;
    private final ExchangeVideoReviewJpaRepository videoReviewRepo;

    public ExchangeUniversityPersistenceAdapter(ExchangeUniversityJpaRepository exchangeUniversityRepo,
                                                 ExchangeVideoReviewJpaRepository videoReviewRepo) {
        this.exchangeUniversityRepo = exchangeUniversityRepo;
        this.videoReviewRepo = videoReviewRepo;
    }

    @Override
    public Optional<ExchangeUniversity> findById(ExchangeUniversityId id) {
        return exchangeUniversityRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<ExchangeUniversity> search(String keyword, String countryCode, int offset, int limit) {
        return exchangeUniversityRepo.search(blankToNull(keyword), blankToNull(countryCode), limit, offset)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long count(String keyword, String countryCode) {
        return exchangeUniversityRepo.countSearch(blankToNull(keyword), blankToNull(countryCode));
    }

    @Override
    public List<VideoReview> findReviewsByUniversityId(ExchangeUniversityId id, String direction, int offset, int limit) {
        return videoReviewRepo.findByUniversityIdAndDirection(id.value(), blankToNull(direction), limit, offset)
                .stream().map(this::toReviewDomain).toList();
    }

    @Override
    public long countReviewsByUniversityId(ExchangeUniversityId id, String direction) {
        return videoReviewRepo.countByUniversityIdAndDirection(id.value(), blankToNull(direction));
    }

    @Override
    public Double avgRatingByUniversityId(ExchangeUniversityId id) {
        return videoReviewRepo.avgRatingByUniversityId(id.value());
    }

    @Override
    public Map<Long, Long> countReviewsByUniversityIds(List<Long> universityIds) {
        if (universityIds.isEmpty()) return Map.of();
        return videoReviewRepo.countByUniversityIdIn(universityIds).stream()
                .collect(toMap(
                        p -> p.getUniversityId(),
                        p -> p.getReviewCount()
                ));
    }

    @Override
    public Map<Long, Double> avgRatingsByUniversityIds(List<Long> universityIds) {
        if (universityIds.isEmpty()) return Map.of();
        return videoReviewRepo.avgRatingByUniversityIdIn(universityIds).stream()
                .collect(toMap(
                        p -> p.getUniversityId(),
                        p -> p.getAvgRating()
                ));
    }

    @Override
    public List<ExchangeUniversityCountryResult> findCountries() {
        return exchangeUniversityRepo.findCountries();
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private ExchangeUniversity toDomain(ExchangeUniversityJpaEntity e) {
        return ExchangeUniversity.restore(e.getId(), e.getNameKo(), e.getNameEn(),
                e.getCountry(), e.getCountryCode(), e.getCity(), e.getWebsite(), e.getGlobalUnivId());
    }

    private VideoReview toReviewDomain(ExchangeVideoReviewJpaEntity e) {
        return VideoReview.builder()
                .id(new VideoReviewId(e.getId()))
                .exchangeUniversityId(e.getUniversityId() != null ? new ExchangeUniversityId(e.getUniversityId()) : null)
                .youtubeUrl(e.getYoutubeUrl())
                .title(e.getTitle())
                .channel(e.getChannel())
                .publishedAt(e.getPublishedAt())
                .country(e.getCountry())
                .city(e.getCity())
                .overallRating(e.getOverallRating())
                .difficulty(e.getDifficulty())
                .workload(e.getWorkload())
                .recommend(e.getRecommend())
                .overallTone(e.getOverallTone())
                .excitementLevel(e.getExcitementLevel())
                .costTotal(e.getCostTotal())
                .costRent(e.getCostRent())
                .costFood(e.getCostFood())
                .costTransport(e.getCostTransport())
                .costCurrency(e.getCostCurrency())
                .visaType(e.getVisaType())
                .visaCost(e.getVisaCost())
                .visaDuration(e.getVisaDuration())
                .visaProcessingDays(e.getVisaProcessingDays())
                .dormAvailable(e.getDormAvailable())
                .dormType(e.getDormType())
                .dormPrice(e.getDormPrice())
                .gpaRequirement(e.getGpaRequirement())
                .languageReq(e.getLanguageReq())
                .deadlineInfo(e.getDeadlineInfo())
                .sourceLang(e.getSourceLang())
                .qualityScore(e.getQualityScore())
                .summary(e.getSummary())
                .exchangeInfo(e.getExchangeInfo())
                .tags(e.getTags())
                .direction(e.getDirection())
                .build();
    }
}
