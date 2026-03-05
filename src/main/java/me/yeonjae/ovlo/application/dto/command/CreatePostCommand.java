package me.yeonjae.ovlo.application.dto.command;

public record CreatePostCommand(
        Long boardId,
        Long authorId,
        String title,
        String content
) {}
