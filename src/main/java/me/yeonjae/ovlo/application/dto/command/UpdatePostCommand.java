package me.yeonjae.ovlo.application.dto.command;

public record UpdatePostCommand(
        Long postId,
        Long requesterId,
        String title,
        String content
) {}
