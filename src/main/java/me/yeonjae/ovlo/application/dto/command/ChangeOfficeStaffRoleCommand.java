package me.yeonjae.ovlo.application.dto.command;

import me.yeonjae.ovlo.domain.office.model.OfficeRole;

public record ChangeOfficeStaffRoleCommand(Long officeId, Long memberId, OfficeRole role) {
}
