package me.yeonjae.ovlo.application.port.in.post;

import me.yeonjae.ovlo.application.dto.command.CreateCommentCommand;
import me.yeonjae.ovlo.application.dto.result.CommentResult;

public interface CreateCommentUseCase {
    CommentResult createComment(CreateCommentCommand command);
}
