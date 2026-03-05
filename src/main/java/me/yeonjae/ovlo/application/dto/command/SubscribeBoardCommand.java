package me.yeonjae.ovlo.application.dto.command;

public record SubscribeBoardCommand(
        Long boardId,
        Long memberId
) {}
