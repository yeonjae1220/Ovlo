package me.yeonjae.ovlo.application.service;

import me.yeonjae.ovlo.application.port.in.office.ManageOfficePartnerSettingsUseCase;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficePort;
import me.yeonjae.ovlo.application.port.out.office.SaveOfficePort;
import me.yeonjae.ovlo.domain.office.exception.OfficeException;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/** 대학별 제휴 노출 on/off. 권한(OFFICE_ADMIN)은 컨트롤러의 OfficeAccessPolicy 가 본다. */
@Service
@Transactional
public class OfficePartnerSettingsService implements ManageOfficePartnerSettingsUseCase {

    private final LoadOfficePort loadOfficePort;
    private final SaveOfficePort saveOfficePort;

    public OfficePartnerSettingsService(LoadOfficePort loadOfficePort, SaveOfficePort saveOfficePort) {
        this.loadOfficePort = loadOfficePort;
        this.saveOfficePort = saveOfficePort;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<PartnerCategory> enabledCategories(Long officeId) {
        return require(officeId).getEnabledPartnerCategories();
    }

    @Override
    public Set<PartnerCategory> update(Long officeId, Set<PartnerCategory> enabled) {
        Office office = require(officeId);
        for (PartnerCategory c : PartnerCategory.values()) {
            office.setPartnerEnabled(c, enabled != null && enabled.contains(c));
        }
        return saveOfficePort.save(office).getEnabledPartnerCategories();
    }

    private Office require(Long officeId) {
        return loadOfficePort.findById(officeId)
                .orElseThrow(() -> new OfficeException("국제처를 찾을 수 없습니다: id=" + officeId,
                        OfficeException.ErrorType.NOT_FOUND));
    }
}
