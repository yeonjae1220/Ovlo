package me.yeonjae.ovlo.application.port.in.auth;

import me.yeonjae.ovlo.application.dto.command.GoogleLoginCommand;
import me.yeonjae.ovlo.application.dto.result.GoogleLoginResult;

public interface GoogleLoginUseCase {
    GoogleLoginResult loginWithGoogle(GoogleLoginCommand command);
}
