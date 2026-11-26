package me.yeonjae.ovlo.application.port.in.member;

import me.yeonjae.ovlo.application.dto.command.AdminWithdrawMemberCommand;

/** 관리자 수동 탈퇴. 본인 확인은 관리자가 문의 채널에서 먼저 끝낸 뒤 부른다. */
public interface AdminWithdrawMemberUseCase {
    void withdrawByAdmin(AdminWithdrawMemberCommand command);
}
