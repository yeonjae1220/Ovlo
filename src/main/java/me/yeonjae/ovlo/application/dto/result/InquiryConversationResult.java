package me.yeonjae.ovlo.application.dto.result;

import java.util.List;

public record InquiryConversationResult(List<Turn> turns, Long consultationId, boolean canContinue) {
    public record Turn(String question, AskResult result) {}
}
