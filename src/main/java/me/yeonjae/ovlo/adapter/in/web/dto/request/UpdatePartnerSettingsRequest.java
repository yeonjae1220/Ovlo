package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotNull;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;

import java.util.Set;

public record UpdatePartnerSettingsRequest(@NotNull Set<PartnerCategory> enabledCategories) {
}
