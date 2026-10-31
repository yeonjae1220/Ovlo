package me.yeonjae.ovlo.application.port.in.office;

import me.yeonjae.ovlo.domain.office.model.PartnerCategory;

import java.util.Set;

/** 국제처 관리자 — 이 대학 학생에게 보여 줄 제휴 분류. */
public interface ManageOfficePartnerSettingsUseCase {
    Set<PartnerCategory> enabledCategories(Long officeId);
    Set<PartnerCategory> update(Long officeId, Set<PartnerCategory> enabledCategories);
}
