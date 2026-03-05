package me.yeonjae.ovlo.application.port.in.auth;

import me.yeonjae.ovlo.application.dto.command.RefreshTokenCommand;
import me.yeonjae.ovlo.application.dto.result.TokenPairResult;

public interface RefreshTokenUseCase {
    TokenPairResult refresh(RefreshTokenCommand command);
}
