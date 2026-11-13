package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.port.out.office.*;
import me.yeonjae.ovlo.domain.office.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.List;

@Service
public class NoticeAnswerFinalizer {
    private final NoticeKnowledgePort knowledge;
    private final LoadNoticePort notices;
    private final SaveInquiryPort inquiries;
    private final NoticeGovernanceService governance;
    private final Clock clock;
    public NoticeAnswerFinalizer(NoticeKnowledgePort knowledge,LoadNoticePort notices,SaveInquiryPort inquiries,NoticeGovernanceService governance,Clock clock) {
        this.knowledge=knowledge;this.notices=notices;this.inquiries=inquiries;this.governance=governance;this.clock=clock;
    }
    @Transactional
    public Inquiry finish(Inquiry answer,List<Notice> candidates,List<NoticeCitation> citations,List<Inquiry> history) {
        knowledge.lockOffice(answer.getOfficeId());
        boolean valid=candidates.stream().allMatch(old -> notices.findById(old.getId()).filter(n ->
                n.getOfficeId().equals(answer.getOfficeId()) && n.getRowVersion()==old.getRowVersion()
                && n.getVersion()==old.getVersion() && n.isGroundingEligible(OfficeDates.today(clock),answer.getLang())
                && governance.eligible(n,OfficeDates.today(clock))).isPresent());
        Inquiry result=valid?answer:Inquiry.noGrounds(answer.getOfficeId(),answer.getMemberId(),answer.getLang(),answer.getQuestion(),
                answer.getCandidateNoticeIds(),answer.getAskedAt(),answer.getLatencyMs(),answer.getModelId());
        if(!history.isEmpty()) result.following(history.getLast());
        Inquiry saved=inquiries.save(result);
        if(valid) knowledge.saveCitations(saved.getId(),citations);
        return saved;
    }
}
