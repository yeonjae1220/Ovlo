package me.yeonjae.ovlo.application.port.in.auth;

import me.yeonjae.ovlo.application.dto.command.AppleLoginCommand;
import me.yeonjae.ovlo.application.dto.result.SocialLoginResult;

public interface AppleLoginUseCase {
    SocialLoginResult loginWithApple(AppleLoginCommand command);
}
