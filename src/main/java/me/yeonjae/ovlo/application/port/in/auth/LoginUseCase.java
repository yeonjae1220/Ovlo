package me.yeonjae.ovlo.application.port.in.auth;

import me.yeonjae.ovlo.application.dto.command.LoginCommand;
import me.yeonjae.ovlo.application.dto.result.TokenPairResult;

public interface LoginUseCase {
    TokenPairResult login(LoginCommand command);
}
