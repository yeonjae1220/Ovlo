package me.yeonjae.ovlo.application.port.in.office;

import me.yeonjae.ovlo.application.dto.result.MyOfficeResult;
import me.yeonjae.ovlo.application.dto.result.OfficeResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface GetOfficeQuery {
    Optional<OfficeResult> findById(Long officeId);
    Page<OfficeResult> findAll(Pageable pageable);

    /** 담당자 소속 → 인증 대학의 Office → 수락 대기 초대 순으로 찾는다. 아무것도 없으면 empty. */
    Optional<MyOfficeResult> findMyOffice(Long memberId);
}
