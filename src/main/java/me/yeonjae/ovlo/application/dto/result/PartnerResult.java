package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.office.model.Partner;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;

import java.time.Instant;

/** 플랫폼 관리자용 제휴 상세. */
public record PartnerResult(Long id, PartnerCategory category, String name, String description, String targetUrl,
                            boolean enabled, int sortOrder, Instant createdAt) {
    public static PartnerResult from(Partner p) {
        return new PartnerResult(p.getId(), p.getCategory(), p.getName(), p.getDescription(), p.getTargetUrl(),
                p.isEnabled(), p.getSortOrder(), p.getCreatedAt());
    }
}
