package me.yeonjae.ovlo.application.port.in.office;

import me.yeonjae.ovlo.application.dto.result.InquiryConversationResult;

public interface GetInquiryConversationQuery {
    InquiryConversationResult conversation(Long officeId, Long memberId, Long inquiryId);
}
