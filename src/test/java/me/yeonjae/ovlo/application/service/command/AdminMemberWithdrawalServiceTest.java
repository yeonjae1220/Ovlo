package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.AdminWithdrawMemberCommand;
import me.yeonjae.ovlo.application.dto.command.WithdrawMemberCommand;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.domain.member.exception.MemberException;
import me.yeonjae.ovlo.domain.member.model.Email;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.domain.member.model.MemberStatus;
import me.yeonjae.ovlo.domain.member.model.OAuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AdminMemberWithdrawalServiceTest {

    private static final long MEMBER_ID = 5L;

    @Mock private LoadMemberPort loadMemberPort;
    @Mock private MemberCommandService memberCommandService;

    @InjectMocks
    private AdminMemberWithdrawalService sut;

    @Test
    @DisplayName("Apple 회원도 Apple 재인증 없이 DB 탈퇴를 진행한다")
    void appleMember_withdrawnWithoutReauth() {
        givenMember(OAuthProvider.APPLE, "001234.apple.sub", MemberRole.MEMBER);

        sut.withdrawByAdmin(new AdminWithdrawMemberCommand(MEMBER_ID, "Apple ID 분실, 문의 #12", "admin"));

        then(memberCommandService).should().withdraw(new WithdrawMemberCommand(MEMBER_ID));
    }

    @Test
    @DisplayName("사유가 비었거나 500자를 넘으면 거부하고 회원을 조회하지도 않는다")
    void invalidReason_rejected() {
        assertThatThrownBy(() -> sut.withdrawByAdmin(new AdminWithdrawMemberCommand(MEMBER_ID, "  ", "admin")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sut.withdrawByAdmin(new AdminWithdrawMemberCommand(MEMBER_ID, null, "admin")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sut.withdrawByAdmin(
                new AdminWithdrawMemberCommand(MEMBER_ID, "x".repeat(501), "admin")))
                .isInstanceOf(IllegalArgumentException.class);

        then(loadMemberPort).should(never()).findById(any());
        then(memberCommandService).should(never()).withdraw(any());
    }

    @Test
    @DisplayName("ADMIN 역할 회원은 이 경로로 탈퇴시킬 수 없다")
    void adminTarget_forbidden() {
        givenMember(OAuthProvider.LOCAL, null, MemberRole.ADMIN);

        assertThatThrownBy(() -> sut.withdrawByAdmin(new AdminWithdrawMemberCommand(MEMBER_ID, "사유", "admin")))
                .isInstanceOf(MemberException.class);
        then(memberCommandService).should(never()).withdraw(any());
    }

    @Test
    @DisplayName("없는 회원은 NOT_FOUND")
    void missingMember_notFound() {
        given(loadMemberPort.findById(new MemberId(MEMBER_ID))).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.withdrawByAdmin(new AdminWithdrawMemberCommand(MEMBER_ID, "사유", "admin")))
                .isInstanceOf(MemberException.class);
        then(memberCommandService).should(never()).withdraw(any());
    }

    private void givenMember(OAuthProvider provider, String providerId, MemberRole role) {
        Member member = Member.restore(new MemberId(MEMBER_ID), "ovlo_member", "회원", null,
                new Email("member@example.com"), null, provider, providerId,
                null, null, MemberStatus.ACTIVE, null, null, null,
                List.of(), List.of(), List.of(), role);
        given(loadMemberPort.findById(new MemberId(MEMBER_ID))).willReturn(Optional.of(member));
    }
}
