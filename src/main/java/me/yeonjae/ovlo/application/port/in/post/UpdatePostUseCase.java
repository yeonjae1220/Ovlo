package me.yeonjae.ovlo.application.port.in.post;

import me.yeonjae.ovlo.application.dto.command.UpdatePostCommand;
import me.yeonjae.ovlo.application.dto.result.PostResult;

public interface UpdatePostUseCase {
    PostResult update(UpdatePostCommand command);
}
