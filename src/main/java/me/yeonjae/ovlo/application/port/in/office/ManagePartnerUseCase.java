package me.yeonjae.ovlo.application.port.in.office;

import me.yeonjae.ovlo.application.dto.command.UpsertPartnerCommand;
import me.yeonjae.ovlo.application.dto.result.PartnerResult;

import java.util.List;

/** 플랫폼 관리자 — 제휴 링크 등록·수정·켜기/끄기. 대학별 노출은 Office 설정이 따로 정한다. */
public interface ManagePartnerUseCase {
    PartnerResult create(UpsertPartnerCommand command);
    PartnerResult update(Long partnerId, UpsertPartnerCommand command);
    void setEnabled(Long partnerId, boolean enabled);
    List<PartnerResult> findAll();
}
