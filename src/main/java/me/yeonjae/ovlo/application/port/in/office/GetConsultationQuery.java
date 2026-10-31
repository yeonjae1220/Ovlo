package me.yeonjae.ovlo.application.port.in.office;

import me.yeonjae.ovlo.application.dto.result.ConsultationResult;
import me.yeonjae.ovlo.domain.office.model.ConsultationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface GetConsultationQuery {
    Page<ConsultationResult> findForStaff(Long officeId, ConsultationStatus status, Pageable pageable);

    List<ConsultationResult> findMine(Long officeId, Long memberId);

    /** 담당자용 상세. 다른 Office 는 empty. */
    Optional<ConsultationResult> findForStaff(Long officeId, Long consultationId);

    /** 학생용 상세. 본인 것이 아니면 empty. */
    Optional<ConsultationResult> findMine(Long officeId, Long consultationId, Long memberId);
}
