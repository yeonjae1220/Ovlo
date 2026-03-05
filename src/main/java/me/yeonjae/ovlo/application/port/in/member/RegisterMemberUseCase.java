package me.yeonjae.ovlo.application.port.in.member;

import me.yeonjae.ovlo.application.dto.command.RegisterMemberCommand;
import me.yeonjae.ovlo.application.dto.result.MemberResult;

public interface RegisterMemberUseCase {
    MemberResult register(RegisterMemberCommand command);
}
