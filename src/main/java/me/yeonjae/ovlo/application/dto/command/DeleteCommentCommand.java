package me.yeonjae.ovlo.application.dto.command;

public record DeleteCommentCommand(
        Long postId,
        Long commentId,
        Long requesterId
) {}
