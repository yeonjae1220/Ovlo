package me.yeonjae.ovlo.application.port.out.office;

import me.yeonjae.ovlo.domain.office.model.Inquiry;
import me.yeonjae.ovlo.domain.office.model.InquiryFeedback;
import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

public interface LoadInquiryPort {
    Optional<Inquiry> findById(Long inquiryId);

    /** outcome·from·to 는 null 이면 필터하지 않는다. */
    Page<Inquiry> findByOffice(Long officeId, InquiryOutcome outcome, Instant from, Instant to, Pageable pageable);

    long countByOfficeSince(Long officeId, Instant since);

    Optional<InquiryFeedback> findFeedback(Long inquiryId);
}
