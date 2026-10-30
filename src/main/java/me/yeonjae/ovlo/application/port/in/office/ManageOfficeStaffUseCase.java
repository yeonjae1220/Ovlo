package me.yeonjae.ovlo.application.port.in.office;

import me.yeonjae.ovlo.application.dto.command.ChangeOfficeStaffRoleCommand;
import me.yeonjae.ovlo.application.dto.command.InviteOfficeStaffCommand;
import me.yeonjae.ovlo.application.dto.command.RemoveOfficeStaffCommand;
import me.yeonjae.ovlo.application.dto.result.OfficeStaffResult;

/** 담당자 초대·역할 변경·제거. 호출자 범위 검사(OfficeAccessPolicy)는 어댑터가 먼저 한다. */
public interface ManageOfficeStaffUseCase {
    OfficeStaffResult invite(InviteOfficeStaffCommand command);
    void changeRole(ChangeOfficeStaffRoleCommand command);
    void remove(RemoveOfficeStaffCommand command);
}
