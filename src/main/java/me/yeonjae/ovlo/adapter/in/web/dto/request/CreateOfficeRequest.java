package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOfficeRequest(@NotNull Long globalUnivId,
                                  @NotBlank @Size(max = 200) String name) {
}
