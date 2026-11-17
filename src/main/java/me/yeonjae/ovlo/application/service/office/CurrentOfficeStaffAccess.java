package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficeMembershipPort;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.office.exception.OfficeException;
import org.springframework.stereotype.Component;

/** Scalar DB checks bypass request-entry principals and previously loaded entities. */
@Component
public class CurrentOfficeStaffAccess {
    private final LoadMemberPort members;
    private final LoadOfficeMembershipPort memberships;
    public CurrentOfficeStaffAccess(LoadMemberPort members, LoadOfficeMembershipPort memberships) {
        this.members=members; this.memberships=memberships;
    }
    public void check(Long officeId, Long memberId) {
        if (!members.isActive(new MemberId(memberId)) || memberships.findActiveMembership(new MemberId(memberId))
                .filter(m -> m.belongsTo(officeId)).isEmpty())
            throw new OfficeException("현재 담당자 접근 권한이 없습니다", OfficeException.ErrorType.FORBIDDEN);
    }
}
