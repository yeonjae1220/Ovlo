package me.yeonjae.ovlo.application.dto.command;

public record CreateBoardCommand(
        String name,
        String description,
        String category,
        String scope,
        Long creatorId,
        Long universityId,  // scope == UNIVERSITY일 때만 사용, 나머지는 null
        String minTrustLevel // 작성 최소 신뢰 등급(없으면 null → UNVERIFIED=게이트 없음)
) {}
