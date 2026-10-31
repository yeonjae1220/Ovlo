package me.yeonjae.ovlo.application.dto.command;

import me.yeonjae.ovlo.domain.office.model.PartnerCategory;

public record UpsertPartnerCommand(PartnerCategory category, String name, String description, String targetUrl,
                                   int sortOrder) {
}
