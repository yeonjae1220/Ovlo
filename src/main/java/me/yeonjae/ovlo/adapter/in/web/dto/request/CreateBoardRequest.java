package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateBoardRequest(
        @NotBlank String name,
        String description,
        @NotBlank String category,
        @NotBlank String scope,
        Long universityId,
        String minTrustLevel // 작성 최소 신뢰 등급(UNVERIFIED/STUDENT/EXCHANGE_VERIFIED, 없으면 게이트 없음)
) {}
