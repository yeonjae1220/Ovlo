package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;

public record PartnerRequest(
        @NotNull PartnerCategory category,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 1000) String description,
        @NotBlank @Size(max = 1000) @Pattern(regexp = "https?://.+", message = "http(s) URL 이어야 합니다") String targetUrl,
        int sortOrder) {
}
