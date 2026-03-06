package me.yeonjae.ovlo.application.dto.command;

public record ReactToPostCommand(
        Long postId,
        Long memberId,
        String reactionType
) {}
