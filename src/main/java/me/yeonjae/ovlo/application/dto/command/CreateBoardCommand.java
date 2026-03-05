package me.yeonjae.ovlo.application.dto.command;

public record CreateBoardCommand(
        String name,
        String description,
        String category,
        String scope,
        Long creatorId,
        Long universityId   // scope == UNIVERSITY일 때만 사용, 나머지는 null
) {}
