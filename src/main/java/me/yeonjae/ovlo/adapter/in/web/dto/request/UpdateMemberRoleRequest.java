package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotNull;
import me.yeonjae.ovlo.domain.member.model.MemberRole;

public record UpdateMemberRoleRequest(@NotNull MemberRole role) {
}
