package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record CreateChatRoomRequest(
        @NotBlank @Pattern(regexp = "DM|GROUP", message = "type은 DM 또는 GROUP이어야 합니다") String type,
        String name,
        @NotNull @NotEmpty List<Long> participantIds
) {}
