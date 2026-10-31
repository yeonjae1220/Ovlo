package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InquiryFeedbackRequest(@NotNull Boolean helpful, @Size(max = 300) String reason) {
}
