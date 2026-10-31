package me.yeonjae.ovlo.application.port.in.office;

import me.yeonjae.ovlo.application.dto.result.PartnerCardResult;

import java.util.List;

/** 학생 — 소속 대학이 켠 분류의 제휴 카드와 클릭. */
public interface StudentPartnerUseCase {
    List<PartnerCardResult> forStudent(Long officeId, Long memberId);

    record ClickResult(String url, me.yeonjae.ovlo.domain.office.model.PartnerCategory category) {}

    /** 클릭을 집계하고 목적지 URL 을 돌려준다. 플랫폼·대학 어느 쪽이든 꺼져 있으면 FORBIDDEN. */
    ClickResult click(Long officeId, Long partnerId, Long memberId);
}
