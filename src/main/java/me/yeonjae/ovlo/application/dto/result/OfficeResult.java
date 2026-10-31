package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.office.model.OfficeStatus;

import java.time.Instant;
import java.util.List;

/** 관리자·담당자용 Office 상세. 담당자 목록은 REMOVED 를 포함하지 않는다. */
public record OfficeResult(Long id, Long globalUnivId, String universityName, String name,
                           OfficeStatus status, Instant createdAt, List<OfficeStaffResult> staff) {
}
