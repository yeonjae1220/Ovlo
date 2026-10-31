package me.yeonjae.ovlo.application.dto.command;

import me.yeonjae.ovlo.domain.office.model.OfficeRole;

/** email 은 이미 가입한 회원의 이메일이어야 한다. invitedBy 는 플랫폼 관리자 이메일 또는 초대한 담당자의 memberId 문자열. */
public record InviteOfficeStaffCommand(Long officeId, String email, OfficeRole role, String invitedBy) {
}
