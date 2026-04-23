package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.RegisterMemberCommand;
import me.yeonjae.ovlo.application.dto.command.UpdateMemberProfileCommand;
import me.yeonjae.ovlo.application.dto.command.WithdrawMemberCommand;
import me.yeonjae.ovlo.application.dto.result.MemberResult;
import me.yeonjae.ovlo.application.port.out.auth.PasswordHasherPort;
import me.yeonjae.ovlo.application.port.out.member.HideContentByWithdrawnMemberPort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.member.SaveMemberPort;
import me.yeonjae.ovlo.domain.member.exception.MemberException;
import me.yeonjae.ovlo.domain.member.model.*;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberCommandServiceTest {

    @Mock private LoadMemberPort loadMemberPort;
    @Mock private SaveMemberPort saveMemberPort;
    @Mock private PasswordHasherPort passwordHasherPort;
    @Mock private HideContentByWithdrawnMemberPort hideContentByWithdrawnMemberPort;

    @InjectMocks
    private MemberCommandService memberCommandService;

    private RegisterMemberCommand registerCommand;
    private Member savedMember;

    @BeforeEach
    void setUp() {
        registerCommand = new RegisterMemberCommand(
                "yeonjae", "김연재", "Seoul", "test@example.com", "rawPassword123",
                1L, "Computer Science", "BACHELOR", 3);

        savedMember = Member.create(
                "yeonjae", "김연재", "Seoul",
                new Email("test@example.com"),
                new Password("hashedPassword"),
                new UniversityId(1L),
                new Major("Computer Science", DegreeType.BACHELOR, 3));
        savedMember.assignId(new MemberId(1L));
    }

    @Nested
    @DisplayName("회원 등록")
    class Register {

        @Test
        @DisplayName("정상적으로 회원을 등록할 수 있다")
        void shouldRegister_successfully() {
            given(loadMemberPort.existsByEmail("test@example.com")).willReturn(false);
            given(passwordHasherPort.encode("rawPassword123")).willReturn("hashedPassword");
            given(saveMemberPort.save(any(Member.class))).willReturn(savedMember);

            MemberResult result = memberCommandService.register(registerCommand);

            assertThat(result.name()).isEqualTo("김연재");
            assertThat(result.email()).isEqualTo("test@example.com");
            assertThat(result.status()).isEqualTo("ACTIVE");
            verify(saveMemberPort).save(any(Member.class));
        }

        @Test
        @DisplayName("이미 사용 중인 이메일이면 예외가 발생한다")
        void shouldThrow_whenEmailAlreadyExists() {
            given(loadMemberPort.existsByEmail("test@example.com")).willReturn(true);

            assertThatThrownBy(() -> memberCommandService.register(registerCommand))
                    .isInstanceOf(MemberException.class)
                    .hasMessageContaining("이미 사용 중인 이메일입니다");
        }
    }

    @Nested
    @DisplayName("프로필 수정")
    class UpdateProfile {

        @Test
        @DisplayName("이름과 출신지를 수정할 수 있다")
        void shouldUpdateProfile_nameAndHometown() {
            given(loadMemberPort.findById(new MemberId(1L))).willReturn(Optional.of(savedMember));
            given(saveMemberPort.save(any(Member.class))).willReturn(savedMember);

            UpdateMemberProfileCommand command = new UpdateMemberProfileCommand(
                    1L, "newNickname", null);

            MemberResult result = memberCommandService.updateProfile(command);

            assertThat(result).isNotNull();
            verify(saveMemberPort).save(any(Member.class));
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외가 발생한다")
        void shouldThrow_whenMemberNotFound() {
            given(loadMemberPort.findById(new MemberId(999L))).willReturn(Optional.empty());

            UpdateMemberProfileCommand command = new UpdateMemberProfileCommand(
                    999L, "test", null);

            assertThatThrownBy(() -> memberCommandService.updateProfile(command))
                    .isInstanceOf(MemberException.class)
                    .hasMessageContaining("회원을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("회원 탈퇴")
    class Withdraw {

        @Test
        @DisplayName("활성 회원을 탈퇴시킬 수 있다")
        void shouldWithdraw_activeMemeber() {
            given(loadMemberPort.findById(new MemberId(1L))).willReturn(Optional.of(savedMember));
            given(saveMemberPort.save(any(Member.class))).willReturn(savedMember);

            memberCommandService.withdraw(new WithdrawMemberCommand(1L));

            verify(saveMemberPort).save(any(Member.class));
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외가 발생한다")
        void shouldThrow_whenMemberNotFound() {
            given(loadMemberPort.findById(new MemberId(999L))).willReturn(Optional.empty());

            assertThatThrownBy(() -> memberCommandService.withdraw(new WithdrawMemberCommand(999L)))
                    .isInstanceOf(MemberException.class)
                    .hasMessageContaining("회원을 찾을 수 없습니다");
        }
    }
}
