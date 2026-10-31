package me.yeonjae.ovlo.application.service;

import me.yeonjae.ovlo.application.port.out.office.LoadOfficePort;
import me.yeonjae.ovlo.application.port.out.office.SaveOfficePort;
import me.yeonjae.ovlo.domain.office.exception.OfficeException;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.OfficeId;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OfficePartnerSettingsServiceTest {

    @Mock LoadOfficePort loadOfficePort;
    @Mock SaveOfficePort saveOfficePort;

    @Test
    @DisplayName("요청한 집합으로 전체를 덮어쓰고 저장한다 (빠진 분류는 꺼진다)")
    void updateReplacesSet() {
        Office office = Office.create(266L, "국제처", Instant.now());
        office.assignId(new OfficeId(5L));
        office.setPartnerEnabled(PartnerCategory.PICKUP, true);
        given(loadOfficePort.findById(5L)).willReturn(Optional.of(office));
        given(saveOfficePort.save(any())).willAnswer(inv -> inv.getArgument(0));

        var service = new OfficePartnerSettingsService(loadOfficePort, saveOfficePort);
        Set<PartnerCategory> result = service.update(5L, Set.of(PartnerCategory.SIM, PartnerCategory.INSURANCE));

        assertThat(result).containsExactlyInAnyOrder(PartnerCategory.SIM, PartnerCategory.INSURANCE);
        assertThat(service.enabledCategories(5L)).containsExactlyInAnyOrder(PartnerCategory.SIM, PartnerCategory.INSURANCE);
    }

    @Test
    @DisplayName("없는 Office 는 NOT_FOUND")
    void notFound() {
        given(loadOfficePort.findById(9L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> new OfficePartnerSettingsService(loadOfficePort, saveOfficePort).enabledCategories(9L))
                .isInstanceOf(OfficeException.class);
    }
}
