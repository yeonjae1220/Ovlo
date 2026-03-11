package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotNull Long boardId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10000) String content
) {}
