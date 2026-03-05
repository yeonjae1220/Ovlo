package me.yeonjae.ovlo.application.port.in.follow;

import me.yeonjae.ovlo.application.dto.command.FollowCommand;

public interface FollowMemberUseCase {

    void follow(FollowCommand command);
}
