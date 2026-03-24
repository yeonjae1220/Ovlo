package me.yeonjae.ovlo.application.dto.result;

import java.util.List;

/** @deprecated Use {@link PageResult PageResult&lt;VideoReviewResult&gt;} directly */
@Deprecated(forRemoval = true)
public final class VideoReviewPageResult {
    private VideoReviewPageResult() {}

    public static PageResult<VideoReviewResult> of(List<VideoReviewResult> content, long totalElements, int page, int size) {
        return PageResult.of(content, totalElements, page, size);
    }
}
