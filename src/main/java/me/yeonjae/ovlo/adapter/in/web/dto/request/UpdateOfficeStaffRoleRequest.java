package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotNull;
import me.yeonjae.ovlo.domain.office.model.OfficeRole;

public record UpdateOfficeStaffRoleRequest(@NotNull OfficeRole role) {
}
