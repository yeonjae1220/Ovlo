package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.office.model.Partner;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;

/** 학생 화면 제휴 카드. 목적지 URL 은 싣지 않는다 — 클릭 API 가 집계 뒤에 돌려준다(설계 §6.3). */
public record PartnerCardResult(Long id, PartnerCategory category, String name, String description) {
    public static PartnerCardResult from(Partner p) {
        return new PartnerCardResult(p.getId(), p.getCategory(), p.getName(), p.getDescription());
    }
}
