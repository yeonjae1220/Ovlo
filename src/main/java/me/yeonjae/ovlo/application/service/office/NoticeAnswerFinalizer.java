package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.port.out.office.*;
import me.yeonjae.ovlo.domain.office.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

@Service
public class NoticeAnswerFinalizer {
    private final NoticeKnowledgePort knowledge;
    private final LoadNoticePort notices;
    private final SaveInquiryPort inquiries;
    private final NoticeGovernanceService governance;
    private final Clock clock;
    private final OfficePersonalDataPort privacy;
    private final StudentOfficeAccessPolicy access;
    public NoticeAnswerFinalizer(NoticeKnowledgePort knowledge,LoadNoticePort notices,SaveInquiryPort inquiries,NoticeGovernanceService governance,Clock clock,
                                OfficePersonalDataPort privacy,StudentOfficeAccessPolicy access) {
        this.knowledge=knowledge;this.notices=notices;this.inquiries=inquiries;this.governance=governance;this.clock=clock;
        this.privacy=privacy;this.access=access;
    }
    @Transactional
    public Inquiry finish(Inquiry answer,List<Notice> candidates,List<NoticeCitation> citations,List<Inquiry> history) {
        // Same order as consultation creation/erasure: member first, then office/evidence.
        privacy.lockActiveMember(answer.getMemberId());
        knowledge.lockOffice(answer.getOfficeId());
        access.assertMember(answer.getMemberId(),answer.getOfficeId());
        boolean valid=candidates.stream().allMatch(old -> notices.findById(old.getId()).filter(n ->
                n.getOfficeId().equals(answer.getOfficeId()) && sameEvidence(old,n,answer.getLang())
                && n.isGroundingEligible(OfficeDates.today(clock),answer.getLang())
                && governance.eligible(n,OfficeDates.today(clock))).isPresent());
        Inquiry result=valid?answer:Inquiry.noGrounds(answer.getOfficeId(),answer.getMemberId(),answer.getLang(),answer.getQuestion(),
                answer.getCandidateNoticeIds(),answer.getAskedAt(),answer.getLatencyMs(),answer.getModelId(),InquiryRejectionReason.EVIDENCE_CHANGED);
        if(!history.isEmpty()) result.following(history.getLast());
        Inquiry saved=inquiries.save(result);
        if(valid) knowledge.saveCitations(saved.getId(),citations);
        return saved;
    }
    /** DB rowVersion also changes for unrelated translations and expiry reminders.
     * Keep the office lock and current eligibility checks, but compare the evidence actually used.
     * Approval/review metadata stays strict because it is included in the stored citation snapshot.
     */
    private boolean sameEvidence(Notice before, Notice current, NoticeLang lang) {
        return Objects.equals(before.getId(),current.getId())
                && before.getVersion()==current.getVersion()
                && before.getSourceLang()==current.getSourceLang()
                && before.getCategory()==current.getCategory()
                && Objects.equals(before.getTitle(),current.getTitle())
                && Objects.equals(before.getBody(),current.getBody())
                && Objects.equals(before.getSourceUrl(),current.getSourceUrl())
                && Objects.equals(before.getAttachmentMediaIds(),current.getAttachmentMediaIds())
                && Objects.equals(before.getTerm(),current.getTerm())
                && Objects.equals(before.getEffectiveFrom(),current.getEffectiveFrom())
                && Objects.equals(before.getExpiresAt(),current.getExpiresAt())
                && Objects.equals(before.getSource(),current.getSource())
                && Objects.equals(before.getReview(),current.getReview())
                && Objects.equals(before.getApprovedBy(),current.getApprovedBy())
                && Objects.equals(before.getApprovedAt(),current.getApprovedAt())
                && (lang==before.getSourceLang() || sameTranslation(before,current,lang));
    }
    private boolean sameTranslation(Notice before, Notice current, NoticeLang lang) {
        var old=before.getTranslation(lang).orElse(null);
        var fresh=current.getTranslation(lang).orElse(null);
        // A review marker or generation timestamp alone does not alter the text the model saw.
        return old!=null && fresh!=null && old.getSourceVersion()==fresh.getSourceVersion()
                && Objects.equals(old.getTitle(),fresh.getTitle()) && Objects.equals(old.getBody(),fresh.getBody());
    }

}
