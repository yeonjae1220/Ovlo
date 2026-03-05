package me.yeonjae.ovlo.application.port.in.member;

import me.yeonjae.ovlo.application.dto.command.UpdateMemberProfileCommand;
import me.yeonjae.ovlo.application.dto.result.MemberResult;

public interface UpdateMemberProfileUseCase {
    MemberResult updateProfile(UpdateMemberProfileCommand command);
}
