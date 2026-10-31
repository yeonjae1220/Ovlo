package me.yeonjae.ovlo.application.port.out.office;

import me.yeonjae.ovlo.domain.office.model.Partner;

import java.time.Instant;

public interface SavePartnerPort {
    Partner save(Partner partner);

    /** 클릭 1건. 집계 원천, 외부에 넘기지 않는다. */
    void recordClick(Long partnerId, Long officeId, Long memberId, Instant clickedAt);
}
