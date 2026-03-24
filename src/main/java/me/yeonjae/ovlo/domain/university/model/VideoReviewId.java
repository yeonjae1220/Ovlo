package me.yeonjae.ovlo.domain.university.model;

public record VideoReviewId(Long value) {
    public VideoReviewId {
        if (value == null || value < 1) throw new IllegalArgumentException("VideoReviewId must be positive");
    }
}
