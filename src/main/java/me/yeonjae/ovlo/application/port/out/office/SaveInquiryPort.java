package me.yeonjae.ovlo.application.port.out.office;

import me.yeonjae.ovlo.domain.office.model.Inquiry;
import me.yeonjae.ovlo.domain.office.model.InquiryFeedback;

public interface SaveInquiryPort {
    Inquiry save(Inquiry inquiry);

    /** 문의당 1건. 있으면 덮어쓴다. */
    void saveFeedback(InquiryFeedback feedback);
}
