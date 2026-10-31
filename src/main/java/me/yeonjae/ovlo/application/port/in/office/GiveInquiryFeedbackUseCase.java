package me.yeonjae.ovlo.application.port.in.office;

import me.yeonjae.ovlo.application.dto.command.InquiryFeedbackCommand;

public interface GiveInquiryFeedbackUseCase {
    void feedback(InquiryFeedbackCommand command);
}
