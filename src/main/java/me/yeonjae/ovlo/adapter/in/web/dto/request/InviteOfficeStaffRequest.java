package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import me.yeonjae.ovlo.domain.office.model.OfficeRole;

public record InviteOfficeStaffRequest(@NotBlank @Email String email, @NotNull OfficeRole role) {
}
