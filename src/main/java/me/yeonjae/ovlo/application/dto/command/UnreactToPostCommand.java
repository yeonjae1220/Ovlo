package me.yeonjae.ovlo.application.dto.command;

public record UnreactToPostCommand(
        Long postId,
        Long memberId
) {}
