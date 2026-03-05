package me.yeonjae.ovlo.application.port.in.auth;

import me.yeonjae.ovlo.application.dto.command.LogoutCommand;

public interface LogoutUseCase {
    void logout(LogoutCommand command);
}
