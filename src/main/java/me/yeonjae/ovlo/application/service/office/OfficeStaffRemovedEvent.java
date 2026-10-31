package me.yeonjae.ovlo.application.service.office;

/** 담당자가 제거됐다 — 커밋 뒤 열린 상담방에서 빼야 한다. */
public record OfficeStaffRemovedEvent(Long officeId, Long memberId) {
}
