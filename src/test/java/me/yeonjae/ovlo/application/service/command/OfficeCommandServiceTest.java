package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.ChangeOfficeStaffRoleCommand;
import me.yeonjae.ovlo.application.dto.command.CreateOfficeCommand;
import me.yeonjae.ovlo.application.dto.command.InviteOfficeStaffCommand;
import me.yeonjae.ovlo.application.dto.command.RemoveOfficeStaffCommand;
import me.yeonjae.ovlo.application.dto.result.OfficeStaffResult;
import me.yeonjae.ovlo.application.port.out.auth.TokenStorePort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficePort;
import me.yeonjae.ovlo.application.port.out.office.OfficeNotificationPort;
import me.yeonjae.ovlo.application.port.out.office.SaveOfficePort;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityPort;
import me.yeonjae.ovlo.application.service.office.OfficeResultMapper;
import me.yeonjae.ovlo.domain.member.model.DegreeType;
import me.yeonjae.ovlo.domain.member.model.Email;
import me.yeonjae.ovlo.domain.member.model.Major;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.Password;
import me.yeonjae.ovlo.domain.office.exception.OfficeException;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.OfficeId;
import me.yeonjae.ovlo.domain.office.model.OfficeMembership;
import me.yeonjae.ovlo.domain.office.model.OfficeRole;
import me.yeonjae.ovlo.domain.office.model.OfficeStaffStatus;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OfficeCommandServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T09:00:00Z");
    private static final Long UNIV = 266L;
    private static final Long OFFICE_ID = 5L;

    @Mock LoadOfficePort loadOfficePort;
    @Mock SaveOfficePort saveOfficePort;
    @Mock LoadUniversityPort loadUniversityPort;
    @Mock LoadMemberPort loadMemberPort;
    @Mock OfficeNotificationPort notificationPort;
    @Mock TokenStorePort tokenStorePort;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;

    private OfficeCommandService sut;

    @BeforeEach
    void setUp() {
        OfficeResultMapper mapper = new OfficeResultMapper(loadUniversityPort, loadMemberPort);
        sut = new OfficeCommandService(loadOfficePort, saveOfficePort, loadUniversityPort, loadMemberPort,
                notificationPort, tokenStorePort, mapper, eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().when(saveOfficePort.save(any(Office.class))).thenAnswer(inv -> {
            Office o = inv.getArgument(0);
            if (o.getId() == null) o.assignId(new OfficeId(OFFICE_ID));
            return o;
        });
        lenient().when(loadMemberPort.findAllByIds(anyList())).thenReturn(List.of());
    }

    private static Member member(long id, String email) {
        Member m = Member.create("nick" + id, "이름", "서울", new Email(email), new Password("hashed-pw"),
                new UniversityId(UNIV), new Major("컴퓨터공학", DegreeType.BACHELOR, 2));
        m.assignId(new MemberId(id));
        return m;
    }

    private static Office officeWithAdmin(long adminId) {
        Office office = Office.create(UNIV, "국제처", NOW);
        office.assignId(new OfficeId(OFFICE_ID));
        office.invite(adminId, OfficeRole.OFFICE_ADMIN, "admin@ovlo.test", NOW);
        office.accept(adminId, NOW);
        return office;
    }

    @Nested
    class Create {
        @Test
        @DisplayName("존재하는 대학에 Office 가 없으면 PILOT 으로 만들고 결과를 돌려준다")
        void createsOffice() {
            given(loadUniversityPort.existsById(new UniversityId(UNIV))).willReturn(true);
            given(loadOfficePort.findByUniversityId(UNIV)).willReturn(Optional.empty());
            given(loadUniversityPort.findById(new UniversityId(UNIV))).willReturn(Optional.empty());

            var result = sut.create(new CreateOfficeCommand(UNIV, "성균관대 국제처"));

            assertThat(result.id()).isEqualTo(OFFICE_ID);
            assertThat(result.globalUnivId()).isEqualTo(UNIV);
            assertThat(result.staff()).isEmpty();
            verify(saveOfficePort).save(any(Office.class));
        }

        @Test
        @DisplayName("없는 대학이면 BAD_REQUEST, 이미 Office 가 있으면 CONFLICT")
        void rejectsUnknownUniversityAndDuplicate() {
            given(loadUniversityPort.existsById(new UniversityId(1L))).willReturn(false);
            assertThatThrownBy(() -> sut.create(new CreateOfficeCommand(1L, "x")))
                    .isInstanceOf(OfficeException.class)
                    .extracting(e -> ((OfficeException) e).getErrorType())
                    .isEqualTo(OfficeException.ErrorType.BAD_REQUEST);

            given(loadUniversityPort.existsById(new UniversityId(UNIV))).willReturn(true);
            given(loadOfficePort.findByUniversityId(UNIV)).willReturn(Optional.of(officeWithAdmin(10L)));
            assertThatThrownBy(() -> sut.create(new CreateOfficeCommand(UNIV, "x")))
                    .isInstanceOf(OfficeException.class)
                    .extracting(e -> ((OfficeException) e).getErrorType())
                    .isEqualTo(OfficeException.ErrorType.CONFLICT);
            verify(saveOfficePort, never()).save(any());
        }
    }

    @Nested
    class Invite {
        @Test
        @DisplayName("가입된 회원을 이메일로 초대하면 INVITED 로 저장하고 알림을 보낸다")
        void invitesExistingMember() {
            Office office = officeWithAdmin(10L);
            given(loadOfficePort.findById(OFFICE_ID)).willReturn(Optional.of(office));
            given(loadMemberPort.findByEmail("staff@univ.ac.kr")).willReturn(Optional.of(member(11L, "staff@univ.ac.kr")));

            OfficeStaffResult result = sut.invite(new InviteOfficeStaffCommand(OFFICE_ID, "staff@univ.ac.kr",
                    OfficeRole.OFFICE_STAFF, "10"));

            assertThat(result.memberId()).isEqualTo(11L);
            assertThat(result.email()).isEqualTo("staff@univ.ac.kr");
            assertThat(result.status()).isEqualTo(OfficeStaffStatus.INVITED);
            verify(saveOfficePort).save(office);
            verify(notificationPort).sendStaffInvitation("staff@univ.ac.kr", "국제처", "10");
        }

        @Test
        @DisplayName("가입되지 않은 이메일은 BAD_REQUEST 이고 아무것도 저장하지 않는다")
        void rejectsUnknownEmail() {
            given(loadOfficePort.findById(OFFICE_ID)).willReturn(Optional.of(officeWithAdmin(10L)));
            given(loadMemberPort.findByEmail("nobody@univ.ac.kr")).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.invite(new InviteOfficeStaffCommand(OFFICE_ID, "nobody@univ.ac.kr",
                    OfficeRole.OFFICE_STAFF, "10")))
                    .isInstanceOf(OfficeException.class)
                    .extracting(e -> ((OfficeException) e).getErrorType())
                    .isEqualTo(OfficeException.ErrorType.BAD_REQUEST);
            verify(saveOfficePort, never()).save(any());
            verify(notificationPort, never()).sendStaffInvitation(any(), any(), any());
        }

        @Test
        @DisplayName("없는 Office 는 NOT_FOUND")
        void unknownOffice() {
            given(loadOfficePort.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.invite(new InviteOfficeStaffCommand(99L, "a@b.co", OfficeRole.OFFICE_STAFF, "x")))
                    .isInstanceOf(OfficeException.class)
                    .extracting(e -> ((OfficeException) e).getErrorType())
                    .isEqualTo(OfficeException.ErrorType.NOT_FOUND);
        }
    }

    @Nested
    class Accept {
        @Test
        @DisplayName("초대를 수락하면 ACTIVE 소속을 돌려주고 기존 세션을 지운다(새 클레임으로 재로그인)")
        void acceptsAndRevokesSession() {
            Office office = officeWithAdmin(10L);
            office.invite(11L, OfficeRole.OFFICE_STAFF, "10", NOW);
            given(loadOfficePort.findOfficeWithInvitationFor(11L)).willReturn(Optional.of(office));

            OfficeMembership membership = sut.accept(11L);

            assertThat(membership).isEqualTo(new OfficeMembership(OFFICE_ID, OfficeRole.OFFICE_STAFF));
            verify(saveOfficePort).save(office);
            verify(tokenStorePort).delete(new MemberId(11L));
        }

        @Test
        @DisplayName("초대가 없으면 NOT_FOUND")
        void noInvitation() {
            given(loadOfficePort.findOfficeWithInvitationFor(11L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.accept(11L)).isInstanceOf(OfficeException.class);
            verify(tokenStorePort, never()).delete(any());
        }
    }

    @Nested
    class ChangeRoleAndRemove {
        @Test
        @DisplayName("제거하면 REMOVED 로 저장하고 그 회원 세션을 지운다")
        void removeRevokesSession() {
            Office office = officeWithAdmin(10L);
            office.invite(11L, OfficeRole.OFFICE_STAFF, "10", NOW);
            office.accept(11L, NOW);
            given(loadOfficePort.findById(OFFICE_ID)).willReturn(Optional.of(office));

            sut.remove(new RemoveOfficeStaffCommand(OFFICE_ID, 11L));

            assertThat(office.findStaff(11L).orElseThrow().getStatus()).isEqualTo(OfficeStaffStatus.REMOVED);
            verify(saveOfficePort).save(office);
            verify(tokenStorePort).delete(new MemberId(11L));
        }

        @Test
        @DisplayName("마지막 관리자 제거는 도메인 규칙으로 막히고 저장·세션 삭제가 없다")
        void lastAdminProtected() {
            given(loadOfficePort.findById(OFFICE_ID)).willReturn(Optional.of(officeWithAdmin(10L)));

            assertThatThrownBy(() -> sut.remove(new RemoveOfficeStaffCommand(OFFICE_ID, 10L)))
                    .isInstanceOf(OfficeException.class);
            assertThatThrownBy(() -> sut.changeRole(new ChangeOfficeStaffRoleCommand(OFFICE_ID, 10L, OfficeRole.OFFICE_STAFF)))
                    .isInstanceOf(OfficeException.class);
            verify(saveOfficePort, never()).save(any());
            verify(tokenStorePort, never()).delete(any());
        }

        @Test
        @DisplayName("역할을 바꾸면 저장하고 세션을 지운다")
        void changeRoleRevokesSession() {
            Office office = officeWithAdmin(10L);
            office.invite(11L, OfficeRole.OFFICE_STAFF, "10", NOW);
            office.accept(11L, NOW);
            given(loadOfficePort.findById(OFFICE_ID)).willReturn(Optional.of(office));

            sut.changeRole(new ChangeOfficeStaffRoleCommand(OFFICE_ID, 11L, OfficeRole.OFFICE_ADMIN));

            assertThat(office.findStaff(11L).orElseThrow().getRole()).isEqualTo(OfficeRole.OFFICE_ADMIN);
            verify(tokenStorePort).delete(new MemberId(11L));
        }
    }

    @Test
    @DisplayName("정지하면 활동 담당자 전원의 세션을 지운다")
    void suspendRevokesAllActiveStaffSessions() {
        Office office = officeWithAdmin(10L);
        office.invite(11L, OfficeRole.OFFICE_STAFF, "10", NOW);
        office.accept(11L, NOW);
        office.invite(12L, OfficeRole.OFFICE_STAFF, "10", NOW); // 미수락
        given(loadOfficePort.findById(OFFICE_ID)).willReturn(Optional.of(office));

        sut.suspend(OFFICE_ID);

        assertThat(office.isSuspended()).isTrue();
        verify(tokenStorePort).delete(new MemberId(10L));
        verify(tokenStorePort).delete(new MemberId(11L));
        verify(tokenStorePort, never()).delete(new MemberId(12L));
    }
}
