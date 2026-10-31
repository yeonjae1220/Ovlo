package me.yeonjae.ovlo.adapter.in.boot;

import me.yeonjae.ovlo.application.dto.command.NoticeActionCommand;
import me.yeonjae.ovlo.application.dto.command.RegisterNoticeCommand;
import me.yeonjae.ovlo.application.dto.result.NoticeResult;
import me.yeonjae.ovlo.application.dto.result.OfficeResult;
import me.yeonjae.ovlo.application.dto.result.PartnerResult;
import me.yeonjae.ovlo.application.dto.result.NoticeSummaryResult;
import me.yeonjae.ovlo.application.port.in.office.AdministerOfficeUseCase;
import me.yeonjae.ovlo.application.port.in.office.GetNoticeQuery;
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
import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeStatus;
import me.yeonjae.ovlo.domain.office.model.OfficeId;
import me.yeonjae.ovlo.domain.office.model.OfficeRole;
import me.yeonjae.ovlo.domain.office.model.OfficeStatus;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    @Mock GetNoticeQuery noticeQuery;
    @Mock ManagePartnerUseCase managePartner;
    @Mock LoadOfficePort loadOfficePort;
    @Mock LoadMemberPort loadMemberPort;

    private PilotSeedRunner runner(long univ, String adminEmail) {
        return new PilotSeedRunner(administerOffice, manageStaff, manageNotice, noticeQuery, managePartner, loadOfficePort, loadMemberPort,
                Clock.fixed(NOW, ZoneOffset.UTC), univ, "파일럿 국제처", adminEmail);
    }

    private static Member admin() {
        Member admin = Member.create("admin", "관리자", "Seoul", new Email("admin@univ.ac.kr"), new Password("x"),
                new UniversityId(266L), new Major("CS", DegreeType.BACHELOR, 1));
        admin.assignId(new MemberId(42L));
        return admin;
    }

    private static Office existingOffice(boolean withAdmin) {
        Office office = Office.create(266L, "x", NOW);
        office.assignId(new OfficeId(5L));
        if (withAdmin) office.invite(42L, OfficeRole.OFFICE_ADMIN, "seed", NOW);
        return office;
    }

    private static NoticeSummaryResult sample(long id, String title, NoticeStatus status) {
        return new NoticeSummaryResult(id, NoticeCategory.ARRIVAL, title, "2027-1", null, null, status, 1, 0, 0, NOW);
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
        given(loadMemberPort.findByEmail("admin@univ.ac.kr")).willReturn(Optional.of(admin()));
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
    @DisplayName("Office·관리자·자료가 이미 있으면 아무것도 만들지 않는다 (멱등)")
    void idempotent() {
        given(managePartner.findAll()).willReturn(List.of(
                new PartnerResult(1L, PartnerCategory.SIM, "KT", null, "https://kt", false, 0, NOW)));
        given(loadOfficePort.findByUniversityId(266L)).willReturn(Optional.of(existingOffice(true)));
        given(loadMemberPort.findByEmail("admin@univ.ac.kr")).willReturn(Optional.of(admin()));
        given(noticeQuery.findForStaff(eq(5L), isNull(), isNull(), any())).willReturn(new PageImpl<>(List.of(
                sample(1L, "[예시] 입국 후 14일 안에 할 일", NoticeStatus.APPROVED),
                sample(2L, "[예시] 기숙사 신청 안내", NoticeStatus.APPROVED),
                sample(3L, "[예시] 유학생 건강보험 가입", NoticeStatus.APPROVED),
                sample(4L, "실제 자료", NoticeStatus.APPROVED))));

        runner(266L, "admin@univ.ac.kr").run(null);

        verify(managePartner, never()).create(any());
        verify(administerOffice, never()).create(any());
        verify(manageStaff, never()).invite(any());
        verify(manageNotice, never()).register(any());
        verify(manageNotice, never()).approve(any());
    }

    @Test
    @DisplayName("Office 는 있는데 초대·자료가 없으면(이전 시드가 중간에 죽음) 거기서부터 이어서 만든다")
    void resumesAfterPartialSeed() {
        given(managePartner.findAll()).willReturn(List.of(
                new PartnerResult(1L, PartnerCategory.SIM, "KT", null, "https://kt", false, 0, NOW)));
        given(loadOfficePort.findByUniversityId(266L)).willReturn(Optional.of(existingOffice(false)));
        given(loadMemberPort.findByEmail("admin@univ.ac.kr")).willReturn(Optional.of(admin()));
        given(noticeQuery.findForStaff(eq(5L), isNull(), isNull(), any())).willReturn(Page.empty());
        NoticeResult registered = mock(NoticeResult.class);
        given(registered.id()).willReturn(9L);
        given(manageNotice.register(any())).willReturn(registered);

        runner(266L, "admin@univ.ac.kr").run(null);

        verify(administerOffice, never()).create(any());
        verify(manageStaff).invite(any());
        verify(manageNotice, times(3)).register(any());
        verify(manageNotice, times(3)).approve(new NoticeActionCommand(5L, 9L, "seed:42"));
    }

    @Test
    @DisplayName("샘플 자료가 일부만 있으면 없는 제목만 만들고, DRAFT 로 남은 것은 승인만 한다")
    void finishesPartialSamples() {
        given(managePartner.findAll()).willReturn(List.of(
                new PartnerResult(1L, PartnerCategory.SIM, "KT", null, "https://kt", false, 0, NOW)));
        given(loadOfficePort.findByUniversityId(266L)).willReturn(Optional.of(existingOffice(true)));
        given(loadMemberPort.findByEmail("admin@univ.ac.kr")).willReturn(Optional.of(admin()));
        given(noticeQuery.findForStaff(eq(5L), isNull(), isNull(), any())).willReturn(new PageImpl<>(List.of(
                sample(1L, "[예시] 입국 후 14일 안에 할 일", NoticeStatus.APPROVED),
                sample(2L, "[예시] 기숙사 신청 안내", NoticeStatus.DRAFT))));
        NoticeResult registered = mock(NoticeResult.class);
        given(registered.id()).willReturn(9L);
        given(manageNotice.register(any())).willReturn(registered);

        runner(266L, "admin@univ.ac.kr").run(null);

        verify(manageStaff, never()).invite(any());
        ArgumentCaptor<RegisterNoticeCommand> captor = ArgumentCaptor.forClass(RegisterNoticeCommand.class);
        verify(manageNotice, times(1)).register(captor.capture());
        assertThat(captor.getValue().title()).isEqualTo("[예시] 유학생 건강보험 가입");
        verify(manageNotice).approve(new NoticeActionCommand(5L, 9L, "seed:42"));
        verify(manageNotice).approve(new NoticeActionCommand(5L, 2L, "seed:42"));
        verify(manageNotice, times(2)).approve(any());
    }

    @Test
    @DisplayName("관리자가 REMOVED 상태면 담당자가 아니므로 다시 초대한다")
    void reinvitesRemovedAdmin() {
        given(managePartner.findAll()).willReturn(List.of(
                new PartnerResult(1L, PartnerCategory.SIM, "KT", null, "https://kt", false, 0, NOW)));
        Office office = existingOffice(false);
        office.invite(7L, OfficeRole.OFFICE_ADMIN, "seed", NOW);          // 다른 관리자가 있어야 42 를 뺄 수 있다
        office.accept(7L, NOW);
        office.invite(42L, OfficeRole.OFFICE_STAFF, "seed", NOW);
        office.remove(42L, NOW);
        given(loadOfficePort.findByUniversityId(266L)).willReturn(Optional.of(office));
        given(loadMemberPort.findByEmail("admin@univ.ac.kr")).willReturn(Optional.of(admin()));
        given(noticeQuery.findForStaff(eq(5L), isNull(), isNull(), any())).willReturn(new PageImpl<>(List.of(
                sample(1L, "[예시] 입국 후 14일 안에 할 일", NoticeStatus.APPROVED),
                sample(2L, "[예시] 기숙사 신청 안내", NoticeStatus.APPROVED),
                sample(3L, "[예시] 유학생 건강보험 가입", NoticeStatus.APPROVED))));

        runner(266L, "admin@univ.ac.kr").run(null);

        verify(manageStaff).invite(any());
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
