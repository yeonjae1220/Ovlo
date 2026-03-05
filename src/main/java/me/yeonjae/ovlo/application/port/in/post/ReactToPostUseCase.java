package me.yeonjae.ovlo.application.port.in.post;

import me.yeonjae.ovlo.application.dto.command.ReactToPostCommand;

public interface ReactToPostUseCase {
    void react(ReactToPostCommand command);
    void unreact(ReactToPostCommand command);
}
