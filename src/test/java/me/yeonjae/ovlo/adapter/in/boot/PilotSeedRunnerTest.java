package me.yeonjae.ovlo.adapter.in.boot;

import me.yeonjae.ovlo.application.dto.command.NoticeActionCommand;
import me.yeonjae.ovlo.application.dto.command.RegisterNoticeCommand;
import me.yeonjae.ovlo.application.dto.result.NoticeResult;
import me.yeonjae.ovlo.application.dto.result.OfficeResult;
import me.yeonjae.ovlo.application.dto.result.PartnerResult;
import me.yeonjae.ovlo.application.port.in.office.AdministerOfficeUseCase;
import me.yeonjae.ovlo.application.port.in.office.ManageNoticeUseCase;
import me.yeonjae.ovlo.application.port.in.office.ManageOfficeStaffUseCase;
import me.yeonjae.ovlo.application.port.in.office.ManagePartnerUseCase;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficePort;
import me.yeonjae.ovlo.domain.member.model.DegreeType;
import me.yeonjae.ovlo.domain.member.model.Email;
import me.yeonjae.ovlo.domain.member.model.Major;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.Password;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.OfficeStatus;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PilotSeedRunnerTest {

    private static final Instant NOW = Instant.parse("2027-03-02T00:00:00Z");

    @Mock AdministerOfficeUseCase administerOffice;
    @Mock ManageOfficeStaffUseCase manageStaff;
    @Mock ManageNoticeUseCase manageNotice;
    @Mock ManagePartnerUseCase managePartner;
    @Mock LoadOfficePort loadOfficePort;
    @Mock LoadMemberPort loadMemberPort;

    private PilotSeedRunner runner(long univ, String adminEmail) {
        return new PilotSeedRunner(administerOffice, manageStaff, manageNotice, managePartner, loadOfficePort, loadMemberPort,
                Clock.fixed(NOW, ZoneOffset.UTC), univ, "파일럿 국제처", adminEmail);
    }

    private static OfficeResult office(long id) {
        return new OfficeResult(id, 266L, "대학", "파일럿 국제처", OfficeStatus.PILOT, NOW, List.of());
    }

    @Test
    @DisplayName("Office 가 없으면 제휴 4건·Office·관리자 초대·승인 자료 3건을 만든다")
    void seedsEverything() {
        given(managePartner.findAll()).willReturn(List.of());
        given(loadOfficePort.findByUniversityId(266L)).willReturn(Optional.empty());
        given(administerOffice.create(any())).willReturn(office(5L));
        Member admin = Member.create("admin", "관리자", "Seoul", new Email("admin@univ.ac.kr"), new Password("x"),
                new UniversityId(266L), new Major("CS", DegreeType.BACHELOR, 1));
        admin.assignId(new MemberId(42L));
        given(loadMemberPort.findByEmail("admin@univ.ac.kr")).willReturn(Optional.of(admin));
        NoticeResult registered = mock(NoticeResult.class);
        given(registered.id()).willReturn(9L);
        given(manageNotice.register(any())).willReturn(registered);

        runner(266L, "admin@univ.ac.kr").run(null);

        verify(managePartner, times(4)).create(any());
        verify(manageStaff).invite(any());
        ArgumentCaptor<RegisterNoticeCommand> captor = ArgumentCaptor.forClass(RegisterNoticeCommand.class);
        verify(manageNotice, times(3)).register(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(c -> {
            assertThat(c.officeId()).isEqualTo(5L);
            assertThat(c.createdBy()).isEqualTo(42L);
            assertThat(c.title()).startsWith("[예시]");
            assertThat(c.term()).isEqualTo("2027-1");
        });
        verify(manageNotice, times(3)).approve(new NoticeActionCommand(5L, 9L, "seed:42"));
    }

    @Test
    @DisplayName("Office 가 이미 있으면 제휴 확인 뒤 아무것도 만들지 않는다 (멱등)")
    void idempotent() {
        given(managePartner.findAll()).willReturn(List.of(
                new PartnerResult(1L, PartnerCategory.SIM, "KT", null, "https://kt", false, 0, NOW)));
        given(loadOfficePort.findByUniversityId(266L)).willReturn(Optional.of(Office.create(266L, "x", NOW)));

        runner(266L, "admin@univ.ac.kr").run(null);

        verify(managePartner, never()).create(any());
        verify(administerOffice, never()).create(any());
        verify(manageNotice, never()).register(any());
    }

    @Test
    @DisplayName("관리자 이메일이 회원이 아니면 Office 만 만들고 초대·자료는 건너뛴다; 대학 id 가 없으면 기동 실패")
    void missingAdminOrUniversity() {
        given(managePartner.findAll()).willReturn(List.of());
        given(loadOfficePort.findByUniversityId(266L)).willReturn(Optional.empty());
        given(administerOffice.create(any())).willReturn(office(5L));
        given(loadMemberPort.findByEmail("nobody@x")).willReturn(Optional.empty());

        runner(266L, "nobody@x").run(null);
        verify(manageStaff, never()).invite(any());
        verify(manageNotice, never()).register(any());

        assertThatThrownBy(() -> runner(0L, "").run(null)).isInstanceOf(IllegalStateException.class);
    }
}
