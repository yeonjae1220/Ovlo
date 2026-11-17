package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficeMembershipPort;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.office.model.Consultation;
import me.yeonjae.ovlo.domain.office.exception.OfficeException;
import org.springframework.stereotype.Component;

/** Re-read current authority around external calls; request-entry authentication is only a snapshot. */
@Component
public class ConsultationTranscriptAccess {
    private final LoadMemberPort members;
    private final LoadOfficeMembershipPort memberships;
    public ConsultationTranscriptAccess(LoadMemberPort members, LoadOfficeMembershipPort memberships) {
        this.members=members; this.memberships=memberships;
    }
    public void check(Consultation consultation, Long requester, boolean staff) {
        if (!members.isActive(new MemberId(requester))) throw forbidden();
        if (!staff) {
            // An active student retains access to their own consented conversation after credential expiry.
            if (!consultation.isOwnedBy(requester)) throw forbidden();
            return;
        }
        // Scalar projection bypasses previously loaded Office/Staff entities; suspended offices are excluded.
        if (memberships.findActiveMembership(new MemberId(requester))
                .filter(m -> m.belongsTo(consultation.getOfficeId())).isEmpty())
            throw forbidden();
    }
    private static OfficeException forbidden() {
        return new OfficeException("현재 상담 접근 권한이 없습니다",OfficeException.ErrorType.FORBIDDEN);
    }
}
