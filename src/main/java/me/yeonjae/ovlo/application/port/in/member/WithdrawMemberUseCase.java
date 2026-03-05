package me.yeonjae.ovlo.application.port.in.member;

import me.yeonjae.ovlo.application.dto.command.WithdrawMemberCommand;

public interface WithdrawMemberUseCase {
    void withdraw(WithdrawMemberCommand command);
}
