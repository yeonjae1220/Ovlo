package me.yeonjae.ovlo.domain.office.model;

import java.time.LocalDate;

/** 현재 승인본의 출처 확인 상태. 실패는 확인 기한을 연장하지 않는다. */
public record NoticeReview(int sourceVersion, LocalDate verifiedOn, LocalDate nextReviewOn, String holdReason) {
    public static NoticeReview pending() { return new NoticeReview(0, null, null, "출처 검토 필요"); }
    public boolean eligible(int version, LocalDate today) {
        return sourceVersion == version && verifiedOn != null && !today.isBefore(verifiedOn)
                && nextReviewOn != null && !today.isAfter(nextReviewOn) && holdReason == null;
    }
    public NoticeReview hold(String reason) {
        return new NoticeReview(sourceVersion, verifiedOn, nextReviewOn, NoticeSource.text(reason, "중지 사유", 1000));
    }
}
