package me.yeonjae.ovlo.application.port.in.member;

import me.yeonjae.ovlo.application.dto.command.UpdateProfileImageCommand;
import me.yeonjae.ovlo.application.dto.result.MemberResult;

public interface UpdateProfileImageUseCase {
    MemberResult updateProfileImage(UpdateProfileImageCommand command);
}
