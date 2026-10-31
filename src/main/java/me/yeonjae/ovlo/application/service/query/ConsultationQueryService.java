package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.ConsultationResult;
import me.yeonjae.ovlo.application.port.in.office.GetConsultationQuery;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.office.LoadConsultationPort;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.office.model.Consultation;
import me.yeonjae.ovlo.domain.office.model.ConsultationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ConsultationQueryService implements GetConsultationQuery {

    private final LoadConsultationPort loadConsultationPort;
    private final LoadMemberPort loadMemberPort;

    public ConsultationQueryService(LoadConsultationPort loadConsultationPort, LoadMemberPort loadMemberPort) {
        this.loadConsultationPort = loadConsultationPort;
        this.loadMemberPort = loadMemberPort;
    }

    @Override
    public Page<ConsultationResult> findForStaff(Long officeId, ConsultationStatus status, Pageable pageable) {
        return loadConsultationPort.findByOffice(officeId, status, pageable).map(this::toResult);
    }

    @Override
    public List<ConsultationResult> findMine(Long officeId, Long memberId) {
        return loadConsultationPort.findByMember(officeId, memberId).stream().map(this::toResult).toList();
    }

    @Override
    public Optional<ConsultationResult> findForStaff(Long officeId, Long consultationId) {
        return loadConsultationPort.findById(consultationId)
                .filter(c -> c.getOfficeId().equals(officeId))
                .map(this::toResult);
    }

    @Override
    public Optional<ConsultationResult> findMine(Long officeId, Long consultationId, Long memberId) {
        return loadConsultationPort.findById(consultationId)
                .filter(c -> c.getOfficeId().equals(officeId) && c.isOwnedBy(memberId))
                .map(this::toResult);
    }

    private ConsultationResult toResult(Consultation c) {
        String nickname = c.getAssignedStaffMemberId() == null ? null
                : loadMemberPort.findById(new MemberId(c.getAssignedStaffMemberId())).map(m -> m.getNickname()).orElse(null);
        return ConsultationResult.from(c, nickname);
    }
}
