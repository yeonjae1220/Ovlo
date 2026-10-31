package me.yeonjae.ovlo.application.dto.command;

public record InquiryFeedbackCommand(Long officeId, Long inquiryId, Long memberId, boolean helpful, String reason) {
}
