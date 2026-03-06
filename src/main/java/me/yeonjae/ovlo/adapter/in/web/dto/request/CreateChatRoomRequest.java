package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateChatRoomRequest(
        @NotBlank String type,
        String name,
        @NotNull @NotEmpty List<Long> participantIds
) {}
