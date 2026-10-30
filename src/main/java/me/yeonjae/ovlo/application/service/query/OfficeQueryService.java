package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.MyOfficeResult;
import me.yeonjae.ovlo.application.dto.result.OfficeResult;
import me.yeonjae.ovlo.application.port.in.office.GetOfficeQuery;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficeMembershipPort;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficePort;
import me.yeonjae.ovlo.application.port.out.verification.LoadVerificationCredentialPort;
import me.yeonjae.ovlo.application.service.office.OfficeResultMapper;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.OfficeMembership;
import me.yeonjae.ovlo.domain.verification.model.VerificationCredential;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class OfficeQueryService implements GetOfficeQuery {

    private final LoadOfficePort loadOfficePort;
    private final LoadOfficeMembershipPort loadMembershipPort;
    private final LoadVerificationCredentialPort loadCredentialPort;
    private final OfficeResultMapper mapper;

    public OfficeQueryService(LoadOfficePort loadOfficePort, LoadOfficeMembershipPort loadMembershipPort,
                              LoadVerificationCredentialPort loadCredentialPort, OfficeResultMapper mapper) {
        this.loadOfficePort = loadOfficePort;
        this.loadMembershipPort = loadMembershipPort;
        this.loadCredentialPort = loadCredentialPort;
        this.mapper = mapper;
    }

    @Override
    public Optional<OfficeResult> findById(Long officeId) {
        return loadOfficePort.findById(officeId).map(mapper::toResult);
    }

    @Override
    public Page<OfficeResult> findAll(Pageable pageable) {
        return loadOfficePort.findAll(pageable).map(mapper::toResult);
    }

    @Override
    public Optional<MyOfficeResult> findMyOffice(Long memberId) {
        List<Long> verifiedUniversityIds = loadCredentialPort.findByMemberId(memberId).stream()
                .filter(VerificationCredential::isActive)
                .map(VerificationCredential::getUniversityId)
                .toList();

        // 1) 담당자 소속
        Optional<OfficeMembership> membership = loadMembershipPort.findActiveMembership(new MemberId(memberId));
        if (membership.isPresent()) {
            return loadOfficePort.findById(membership.get().officeId())
                    .map(o -> new MyOfficeResult(o.getId().value(), o.getName(), o.getGlobalUnivId(), o.getStatus(),
                            membership.get().role(), verifiedUniversityIds.contains(o.getGlobalUnivId()), false));
        }
        // 2) 인증 대학의 Office (학생)
        for (Long univId : verifiedUniversityIds) {
            Optional<Office> office = loadOfficePort.findByUniversityId(univId);
            if (office.isPresent()) {
                Office o = office.get();
                return Optional.of(new MyOfficeResult(o.getId().value(), o.getName(), o.getGlobalUnivId(),
                        o.getStatus(), null, true, false));
            }
        }
        // 3) 수락 대기 초대
        return loadOfficePort.findOfficeWithInvitationFor(memberId)
                .map(o -> new MyOfficeResult(o.getId().value(), o.getName(), o.getGlobalUnivId(), o.getStatus(),
                        null, verifiedUniversityIds.contains(o.getGlobalUnivId()), true));
    }
}
