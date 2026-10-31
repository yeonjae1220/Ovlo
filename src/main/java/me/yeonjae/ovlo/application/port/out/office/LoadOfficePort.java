package me.yeonjae.ovlo.application.port.out.office;

import me.yeonjae.ovlo.domain.office.model.Office;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface LoadOfficePort {
    Optional<Office> findById(Long officeId);

    /** 대학당 Office 1개. */
    Optional<Office> findByUniversityId(Long globalUnivId);

    Page<Office> findAll(Pageable pageable);

    /** 이 회원에게 INVITED 상태 초대가 있는 Office. */
    Optional<Office> findOfficeWithInvitationFor(Long memberId);
}
