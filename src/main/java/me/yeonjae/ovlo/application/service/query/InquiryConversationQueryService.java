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
    private final NoticeKnowledgePort knowledge;
    private final me.yeonjae.ovlo.application.service.office.NoticeGovernanceService governance;
    private final java.time.Clock clock;

    public InquiryConversationQueryService(LoadInquiryPort inquiries, LoadNoticePort notices,
                                          LoadConsultationPort consultations, StudentOfficeAccessPolicy access,NoticeKnowledgePort knowledge,
                                          me.yeonjae.ovlo.application.service.office.NoticeGovernanceService governance,java.time.Clock clock) {
        this.inquiries = inquiries; this.notices = notices; this.consultations = consultations; this.access = access;this.knowledge=knowledge;this.governance=governance;this.clock=clock;
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
        for (NoticeCitation snapshot:knowledge.citations(inquiry.getId())) {
            boolean current=notices.findById(snapshot.noticeId()).filter(n -> n.getOfficeId().equals(inquiry.getOfficeId())
                    && n.getVersion()==snapshot.version() && governance.eligible(n,me.yeonjae.ovlo.application.service.office.OfficeDates.today(clock))).isPresent();
            citations.add(AskResult.Citation.from(snapshot,current));
        }
        if(citations.isEmpty()) for(Long id:inquiry.getCitedNoticeIds()) {
            // 과거 근거를 현재 본문으로 재구성하지 않는다.
            citations.add(new AskResult.Citation(inquiry.getCandidateNoticeIds().indexOf(id)+1,id,"과거 출처 (버전 미기록)",null,null));
        }
        return new AskResult(inquiry.getId(), inquiry.getOutcome(), inquiry.getLang(), inquiry.getAnswerText(), citations,
                inquiry.getOutcome() != InquiryOutcome.ESCALATED);
    }
}
