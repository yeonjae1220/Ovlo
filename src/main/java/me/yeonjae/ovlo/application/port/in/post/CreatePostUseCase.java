package me.yeonjae.ovlo.application.port.in.post;

import me.yeonjae.ovlo.application.dto.command.CreatePostCommand;
import me.yeonjae.ovlo.application.dto.result.PostResult;

public interface CreatePostUseCase {
    PostResult create(CreatePostCommand command);
}
