package me.yeonjae.ovlo.application.port.in.office;

import me.yeonjae.ovlo.application.dto.result.InquiryLogResult;
import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

/** 담당자 문의 로그. */
public interface GetInquiryQuery {
    Page<InquiryLogResult> findForStaff(Long officeId, InquiryOutcome outcome, Instant from, Instant to, Pageable pageable);
}
