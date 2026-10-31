package me.yeonjae.ovlo.application.port.out.office;

import me.yeonjae.ovlo.domain.office.model.Consultation;
import me.yeonjae.ovlo.domain.office.model.ConsultationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LoadConsultationPort {
    Optional<Consultation> findById(Long consultationId);

    Optional<Consultation> findByChatRoomId(Long chatRoomId);

    /** status 가 null 이면 전체. */
    Page<Consultation> findByOffice(Long officeId, ConsultationStatus status, Pageable pageable);

    List<Consultation> findByMember(Long officeId, Long memberId);

    /** CLOSED 가 아닌 상담 — 담당자 제거 시 방에서 빼기 위해. */
    List<Consultation> findActiveByOffice(Long officeId);

    /** RESOLVED 이고 resolvedAt <= cutoff 인 상담 id — 자동 종료 대상. */
    List<Long> findIdsResolvedBefore(Instant cutoff);
}
