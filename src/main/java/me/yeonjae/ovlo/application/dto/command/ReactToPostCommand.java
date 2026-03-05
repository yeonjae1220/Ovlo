package me.yeonjae.ovlo.application.dto.command;

public record ReactToPostCommand(
        Long postId,
        Long memberId,
        String reactionType  // "LIKE" | "DISLIKE" | null (unreact 시 사용 안 함)
) {}
