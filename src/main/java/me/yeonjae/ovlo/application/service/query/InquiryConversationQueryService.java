package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.AskResult;
import me.yeonjae.ovlo.application.dto.result.InquiryConversationResult;
import me.yeonjae.ovlo.application.port.in.office.GetInquiryConversationQuery;
import me.yeonjae.ovlo.application.port.out.office.*;
import me.yeonjae.ovlo.application.service.office.InquiryHistory;
import me.yeonjae.ovlo.application.service.office.StudentOfficeAccessPolicy;
import me.yeonjae.ovlo.domain.office.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
public class InquiryConversationQueryService implements GetInquiryConversationQuery {
    private final LoadInquiryPort inquiries;
    private final LoadNoticePort notices;
    private final LoadConsultationPort consultations;
    private final StudentOfficeAccessPolicy access;

    public InquiryConversationQueryService(LoadInquiryPort inquiries, LoadNoticePort notices,
                                          LoadConsultationPort consultations, StudentOfficeAccessPolicy access) {
        this.inquiries = inquiries; this.notices = notices; this.consultations = consultations; this.access = access;
    }

    @Override
    @Transactional(readOnly = true)
    public InquiryConversationResult conversation(Long officeId, Long memberId, Long inquiryId) {
        access.assertMember(memberId, officeId);
        List<Inquiry> history = new ArrayList<>(InquiryHistory.load(inquiries, officeId, memberId, inquiryId));
        // A bookmarked earlier turn also finds replies completed after a lost HTTP response.
        while (history.size() < InquiryHistory.MAX_TURNS) {
            var next = inquiries.findByPreviousInquiryId(history.getLast().getId());
            if (next.isEmpty()) break;
            InquiryHistory.assertOwner(next.get(), officeId, memberId);
            history.add(next.get());
        }
        Inquiry last = history.getLast();
        Long consultationId = consultations.findFirstByInquiryId(last.getId()).map(Consultation::getId).orElse(null);
        return new InquiryConversationResult(history.stream().map(i -> new InquiryConversationResult.Turn(i.getQuestion(), result(i))).toList(),
                consultationId, last.getOutcome() != InquiryOutcome.ESCALATED && history.size() < InquiryHistory.MAX_TURNS);
    }

    private AskResult result(Inquiry inquiry) {
        List<AskResult.Citation> citations = new ArrayList<>();
        for (int n = 0; n < inquiry.getCandidateNoticeIds().size(); n++) {
            Long id = inquiry.getCandidateNoticeIds().get(n);
            if (!inquiry.getCitedNoticeIds().contains(id)) continue;
            final int index = n + 1;
            notices.findById(id).filter(notice -> notice.getOfficeId().equals(inquiry.getOfficeId())).ifPresent(notice ->
                    citations.add(new AskResult.Citation(index, id, notice.getTitle(), notice.getSourceUrl(), notice.getExpiresAt())));
        }
        return new AskResult(inquiry.getId(), inquiry.getOutcome(), inquiry.getLang(), inquiry.getAnswerText(), citations,
                inquiry.getOutcome() != InquiryOutcome.ESCALATED && inquiry.getOutcome() != InquiryOutcome.OUT_OF_SCOPE);
    }
}
