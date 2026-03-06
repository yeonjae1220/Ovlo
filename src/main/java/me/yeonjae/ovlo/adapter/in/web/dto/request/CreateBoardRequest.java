package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateBoardRequest(
        @NotBlank String name,
        String description,
        @NotBlank String category,
        @NotBlank String scope,
        Long universityId
) {}
