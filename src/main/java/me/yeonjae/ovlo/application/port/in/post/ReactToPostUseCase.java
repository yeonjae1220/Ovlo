package me.yeonjae.ovlo.application.port.in.post;

import me.yeonjae.ovlo.application.dto.command.ReactToPostCommand;
import me.yeonjae.ovlo.application.dto.command.UnreactToPostCommand;

public interface ReactToPostUseCase {
    void react(ReactToPostCommand command);
    void unreact(UnreactToPostCommand command);
}
