package me.yeonjae.ovlo.application.service;

import me.yeonjae.ovlo.application.dto.command.UpsertPartnerCommand;
import me.yeonjae.ovlo.application.dto.result.PartnerCardResult;
import me.yeonjae.ovlo.application.dto.result.PartnerResult;
import me.yeonjae.ovlo.application.port.in.office.ManagePartnerUseCase;
import me.yeonjae.ovlo.application.port.in.office.StudentPartnerUseCase;
import me.yeonjae.ovlo.application.port.out.office.LoadPartnerPort;
import me.yeonjae.ovlo.application.port.out.office.SavePartnerPort;
import me.yeonjae.ovlo.application.service.office.StudentOfficeAccessPolicy;
import me.yeonjae.ovlo.domain.office.exception.PartnerException;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.Partner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;

/**
 * 제휴(설계 §4.6·§6.3). 학생에게는 플랫폼에서 켜져 있고 + 그 대학이 켠 분류만 보인다.
 * 클릭은 목적지를 돌려주기 전에 집계한다 — 회원 id 는 집계 원천에만 남고 밖으로 나가지 않는다.
 */
@Service
@Transactional
public class PartnerService implements ManagePartnerUseCase, StudentPartnerUseCase {

    private final LoadPartnerPort loadPartnerPort;
    private final SavePartnerPort savePartnerPort;
    private final StudentOfficeAccessPolicy studentAccessPolicy;
    private final Clock clock;

    public PartnerService(LoadPartnerPort loadPartnerPort, SavePartnerPort savePartnerPort,
                          StudentOfficeAccessPolicy studentAccessPolicy, Clock clock) {
        this.loadPartnerPort = loadPartnerPort;
        this.savePartnerPort = savePartnerPort;
        this.studentAccessPolicy = studentAccessPolicy;
        this.clock = clock;
    }

    // ── 플랫폼 관리자 ──────────────────────────────────────────────────────

    @Override
    public PartnerResult create(UpsertPartnerCommand c) {
        Partner p = Partner.create(c.category(), c.name(), c.description(), c.targetUrl(), c.sortOrder(), clock.instant());
        return PartnerResult.from(savePartnerPort.save(p));
    }

    @Override
    public PartnerResult update(Long partnerId, UpsertPartnerCommand c) {
        Partner p = require(partnerId);
        p.update(c.category(), c.name(), c.description(), c.targetUrl(), c.sortOrder());
        return PartnerResult.from(savePartnerPort.save(p));
    }

    @Override
    public void setEnabled(Long partnerId, boolean enabled) {
        Partner p = require(partnerId);
        if (enabled) p.enable(); else p.disable();
        savePartnerPort.save(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartnerResult> findAll() {
        return loadPartnerPort.findAll().stream().map(PartnerResult::from).toList();
    }

    // ── 학생 ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PartnerCardResult> forStudent(Long officeId, Long memberId) {
        Office office = studentAccessPolicy.assertMember(memberId, officeId);
        return loadPartnerPort.findEnabled().stream()
                .filter(p -> office.isPartnerEnabled(p.getCategory()))
                .sorted(Comparator.comparing(Partner::getCategory).thenComparingInt(Partner::getSortOrder))
                .map(PartnerCardResult::from)
                .toList();
    }

    @Override
    public String click(Long officeId, Long partnerId, Long memberId) {
        Office office = studentAccessPolicy.assertMember(memberId, officeId);
        Partner p = require(partnerId);
        if (!p.isEnabled() || !office.isPartnerEnabled(p.getCategory())) {
            throw new PartnerException("이 대학에서는 제공하지 않는 제휴입니다", PartnerException.ErrorType.FORBIDDEN);
        }
        savePartnerPort.recordClick(partnerId, officeId, memberId, clock.instant());
        return p.getTargetUrl();
    }

    private Partner require(Long partnerId) {
        return loadPartnerPort.findById(partnerId)
                .orElseThrow(() -> new PartnerException("제휴를 찾을 수 없습니다: id=" + partnerId,
                        PartnerException.ErrorType.NOT_FOUND));
    }
}
