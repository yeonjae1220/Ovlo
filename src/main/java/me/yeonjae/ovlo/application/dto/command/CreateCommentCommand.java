package me.yeonjae.ovlo.application.dto.command;

public record CreateCommentCommand(
        Long postId,
        Long authorId,
        String content
) {}
