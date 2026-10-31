package me.yeonjae.ovlo.application.service;

import me.yeonjae.ovlo.application.dto.command.UpsertPartnerCommand;
import me.yeonjae.ovlo.application.dto.result.PartnerCardResult;
import me.yeonjae.ovlo.application.port.out.office.LoadPartnerPort;
import me.yeonjae.ovlo.application.port.out.office.SavePartnerPort;
import me.yeonjae.ovlo.application.service.office.StudentOfficeAccessPolicy;
import me.yeonjae.ovlo.domain.office.exception.PartnerException;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.OfficeId;
import me.yeonjae.ovlo.domain.office.model.Partner;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PartnerServiceTest {

    private static final Instant NOW = Instant.parse("2027-03-02T00:00:00Z");
    private static final long OFFICE = 5L;
    private static final long MEMBER = 42L;

    @Mock LoadPartnerPort loadPartnerPort;
    @Mock SavePartnerPort savePartnerPort;
    @Mock StudentOfficeAccessPolicy accessPolicy;

    private PartnerService service;
    private Office office;

    @BeforeEach
    void setUp() {
        service = new PartnerService(loadPartnerPort, savePartnerPort, accessPolicy, Clock.fixed(NOW, ZoneOffset.UTC));
        office = Office.create(266L, "국제처", NOW);
        office.assignId(new OfficeId(OFFICE));
    }

    private static Partner partner(long id, PartnerCategory category, boolean enabled, int sortOrder) {
        return Partner.restore(id, category, category.name() + id, null, "https://p" + id, enabled, sortOrder, NOW);
    }

    @Test
    @DisplayName("학생에게는 플랫폼에서 켜진 것 중 대학이 켠 분류만, 유심→보험→숙소→픽업·정렬순으로 보인다")
    void forStudentFiltersByOfficeSettings() {
        office.setPartnerEnabled(PartnerCategory.SIM, true);
        office.setPartnerEnabled(PartnerCategory.INSURANCE, true);
        given(accessPolicy.assertMember(MEMBER, OFFICE)).willReturn(office);
        given(loadPartnerPort.findEnabled()).willReturn(List.of(
                partner(1, PartnerCategory.HOUSING, true, 0),
                partner(2, PartnerCategory.INSURANCE, true, 0),
                partner(3, PartnerCategory.SIM, true, 2),
                partner(4, PartnerCategory.SIM, true, 1)));

        List<PartnerCardResult> cards = service.forStudent(OFFICE, MEMBER);

        assertThat(cards).extracting(PartnerCardResult::id).containsExactly(4L, 3L, 2L);
    }

    @Test
    @DisplayName("대학이 아무 분류도 켜지 않았으면 비어 있다")
    void forStudentEmptyByDefault() {
        given(accessPolicy.assertMember(MEMBER, OFFICE)).willReturn(office);
        given(loadPartnerPort.findEnabled()).willReturn(List.of(partner(1, PartnerCategory.SIM, true, 0)));

        assertThat(service.forStudent(OFFICE, MEMBER)).isEmpty();
    }

    @Test
    @DisplayName("클릭은 집계한 뒤 목적지를 돌려주고, 꺼진 제휴나 꺼진 분류면 FORBIDDEN 에 집계도 하지 않는다")
    void click() {
        office.setPartnerEnabled(PartnerCategory.SIM, true);
        given(accessPolicy.assertMember(MEMBER, OFFICE)).willReturn(office);
        given(loadPartnerPort.findById(1L)).willReturn(Optional.of(partner(1, PartnerCategory.SIM, true, 0)));
        given(loadPartnerPort.findById(2L)).willReturn(Optional.of(partner(2, PartnerCategory.SIM, false, 0)));
        given(loadPartnerPort.findById(3L)).willReturn(Optional.of(partner(3, PartnerCategory.HOUSING, true, 0)));
        given(loadPartnerPort.findById(9L)).willReturn(Optional.empty());

        assertThat(service.click(OFFICE, 1L, MEMBER)).isEqualTo("https://p1");
        verify(savePartnerPort).recordClick(1L, OFFICE, MEMBER, NOW);

        assertThatThrownBy(() -> service.click(OFFICE, 2L, MEMBER)).isInstanceOf(PartnerException.class)
                .extracting("errorType").isEqualTo(PartnerException.ErrorType.FORBIDDEN);
        assertThatThrownBy(() -> service.click(OFFICE, 3L, MEMBER)).isInstanceOf(PartnerException.class)
                .extracting("errorType").isEqualTo(PartnerException.ErrorType.FORBIDDEN);
        assertThatThrownBy(() -> service.click(OFFICE, 9L, MEMBER)).isInstanceOf(PartnerException.class)
                .extracting("errorType").isEqualTo(PartnerException.ErrorType.NOT_FOUND);
        // 성공한 1건 말고는 집계가 없다
        verify(savePartnerPort, org.mockito.Mockito.times(1)).recordClick(any(), any(), any(), any());
    }

    @Test
    @DisplayName("관리자 등록은 꺼진 상태로 저장되고, 켜기는 저장을 거친다")
    void adminCreateAndEnable() {
        given(savePartnerPort.save(any())).willAnswer(inv -> {
            Partner p = inv.getArgument(0);
            if (p.getId() == null) p.assignId(10L);
            return p;
        });

        var created = service.create(new UpsertPartnerCommand(PartnerCategory.SIM, "KT", "설명", "https://kt", 0));
        assertThat(created.id()).isEqualTo(10L);
        assertThat(created.enabled()).isFalse();

        given(loadPartnerPort.findById(10L)).willReturn(Optional.of(partner(10, PartnerCategory.SIM, false, 0)));
        service.setEnabled(10L, true);
        verify(savePartnerPort).save(org.mockito.ArgumentMatchers.argThat(p -> p.getId() == 10L && p.isEnabled()));
    }
}
