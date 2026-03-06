package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReactToPostRequest(
        @NotBlank String reactionType
) {}
