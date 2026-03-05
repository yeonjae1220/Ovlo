package me.yeonjae.ovlo.application.dto.command;

public record DeletePostCommand(
        Long postId,
        Long requesterId
) {}
