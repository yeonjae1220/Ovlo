package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.university.model.VideoReview;

import java.time.OffsetDateTime;

public record VideoReviewResult(
        Long id,
        String youtubeUrl,
        String title,
        String channel,
        OffsetDateTime publishedAt,
        String country,
        String city,
        Integer overallRating,
        Integer difficulty,
        Integer workload,
        Boolean recommend,
        String overallTone,
        Integer excitementLevel,
        String costTotal,
        String costRent,
        String costFood,
        String costTransport,
        String costCurrency,
        String visaType,
        String visaCost,
        String visaDuration,
        String visaProcessingDays,
        Boolean dormAvailable,
        String dormType,
        String dormPrice,
        String gpaRequirement,
        String languageReq,
        String deadlineInfo,
        String sourceLang,
        Double qualityScore,
        String summary,
        String exchangeInfo,
        String[] tags
) {
    public static VideoReviewResult from(VideoReview review) {
        return new VideoReviewResult(
                review.getId().value(),
                review.getYoutubeUrl(),
                review.getTitle(),
                review.getChannel(),
                review.getPublishedAt(),
                review.getCountry(),
                review.getCity(),
                review.getOverallRating(),
                review.getDifficulty(),
                review.getWorkload(),
                review.getRecommend(),
                review.getOverallTone(),
                review.getExcitementLevel(),
                review.getCostTotal(),
                review.getCostRent(),
                review.getCostFood(),
                review.getCostTransport(),
                review.getCostCurrency(),
                review.getVisaType(),
                review.getVisaCost(),
                review.getVisaDuration(),
                review.getVisaProcessingDays(),
                review.getDormAvailable(),
                review.getDormType(),
                review.getDormPrice(),
                review.getGpaRequirement(),
                review.getLanguageReq(),
                review.getDeadlineInfo(),
                review.getSourceLang(),
                review.getQualityScore(),
                review.getSummary(),
                review.getExchangeInfo(),
                review.getTags()
        );
    }
}
