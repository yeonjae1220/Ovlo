package me.yeonjae.ovlo.application.port.in.university;

import me.yeonjae.ovlo.application.dto.result.ExchangeUniversityResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.VideoReviewResult;
import me.yeonjae.ovlo.domain.university.model.ExchangeUniversityId;

public interface GetExchangeUniversityQuery {
    ExchangeUniversityResult getById(ExchangeUniversityId id);
    PageResult<VideoReviewResult> getReviews(ExchangeUniversityId id, int page, int size);
}
