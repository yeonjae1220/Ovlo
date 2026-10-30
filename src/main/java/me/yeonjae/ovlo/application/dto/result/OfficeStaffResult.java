package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.office.model.OfficeRole;
import me.yeonjae.ovlo.domain.office.model.OfficeStaffStatus;

import java.time.Instant;

public record OfficeStaffResult(Long memberId, String email, String nickname, OfficeRole role,
                                OfficeStaffStatus status, Instant invitedAt, Instant acceptedAt) {
}
