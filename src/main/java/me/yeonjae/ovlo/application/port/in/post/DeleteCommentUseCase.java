package me.yeonjae.ovlo.application.port.in.post;

import me.yeonjae.ovlo.application.dto.command.DeleteCommentCommand;

public interface DeleteCommentUseCase {
    void deleteComment(DeleteCommentCommand command);
}
