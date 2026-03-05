package me.yeonjae.ovlo.application.port.in.post;

import me.yeonjae.ovlo.application.dto.command.DeletePostCommand;

public interface DeletePostUseCase {
    void delete(DeletePostCommand command);
}
