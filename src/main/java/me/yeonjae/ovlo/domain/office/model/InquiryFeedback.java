package me.yeonjae.ovlo.domain.office.model;

import java.time.Instant;
import java.util.Objects;

/** 답변에 대한 학생의 👍/👎. 문의당 1건, 나중 것이 덮어쓴다. */
public record InquiryFeedback(Long inquiryId, boolean helpful, String reason, Instant createdAt) {

    public static final int MAX_REASON_LENGTH = 300;

    public InquiryFeedback {
        Objects.requireNonNull(inquiryId, "inquiryId는 필수입니다");
        Objects.requireNonNull(createdAt, "createdAt은 필수입니다");
        if (reason != null) {
            reason = reason.isBlank() ? null : reason.trim();
            if (reason != null && reason.length() > MAX_REASON_LENGTH) {
                throw new IllegalArgumentException("사유는 " + MAX_REASON_LENGTH + "자 이하여야 합니다");
            }
        }
    }
}
