package me.yeonjae.ovlo.application.port.out.university;

import me.yeonjae.ovlo.domain.university.model.ExchangeUniversity;
import me.yeonjae.ovlo.domain.university.model.ExchangeUniversityId;
import me.yeonjae.ovlo.domain.university.model.VideoReview;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoadExchangeUniversityPort {
    Optional<ExchangeUniversity> findById(ExchangeUniversityId id);
    List<ExchangeUniversity> search(String keyword, String country, int offset, int limit);
    long count(String keyword, String country);

    List<VideoReview> findReviewsByUniversityId(ExchangeUniversityId id, String direction, int offset, int limit);
    long countReviewsByUniversityId(ExchangeUniversityId id, String direction);
    Double avgRatingByUniversityId(ExchangeUniversityId id);
    Map<Long, Long> countReviewsByUniversityIds(List<Long> universityIds);
    Map<Long, Double> avgRatingsByUniversityIds(List<Long> universityIds);
}
