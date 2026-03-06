package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotNull;

public record FollowRequest(
        @NotNull Long followeeId
) {}
