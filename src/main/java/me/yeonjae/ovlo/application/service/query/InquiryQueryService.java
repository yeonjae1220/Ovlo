package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.InquiryLogResult;
import me.yeonjae.ovlo.application.port.in.office.GetInquiryQuery;
import me.yeonjae.ovlo.application.port.out.office.LoadInquiryPort;
import me.yeonjae.ovlo.domain.office.model.Inquiry;
import me.yeonjae.ovlo.domain.office.model.InquiryFeedback;
import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class InquiryQueryService implements GetInquiryQuery {

    private final LoadInquiryPort loadInquiryPort;

    public InquiryQueryService(LoadInquiryPort loadInquiryPort) {
        this.loadInquiryPort = loadInquiryPort;
    }

    @Override
    public Page<InquiryLogResult> findForStaff(Long officeId, InquiryOutcome outcome, Instant from, Instant to, Pageable pageable) {
        return loadInquiryPort.findByOffice(officeId, outcome, from, to, pageable).map(this::toLog);
    }

    private InquiryLogResult toLog(Inquiry i) {
        Optional<InquiryFeedback> fb = loadInquiryPort.findFeedback(i.getId());
        return new InquiryLogResult(i.getId(), i.getLang(), i.getQuestion(), i.getOutcome(), i.getAnswerText(),
                i.getCitedNoticeIds(), i.getCandidateNoticeIds().size(), i.getAskedAt(), i.getLatencyMs(), i.getModelId(),
                fb.map(InquiryFeedback::helpful).orElse(null), fb.map(InquiryFeedback::reason).orElse(null));
    }
}
