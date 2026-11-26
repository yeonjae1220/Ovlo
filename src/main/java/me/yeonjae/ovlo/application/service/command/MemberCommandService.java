package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.CompleteOnboardingCommand;
import me.yeonjae.ovlo.application.dto.command.RegisterMemberCommand;
import me.yeonjae.ovlo.application.dto.command.UpdateMemberProfileCommand;
import me.yeonjae.ovlo.application.dto.command.UpdateProfileImageCommand;
import me.yeonjae.ovlo.application.dto.command.WithdrawMemberCommand;
import me.yeonjae.ovlo.application.dto.result.MemberResult;
import me.yeonjae.ovlo.application.port.in.member.CompleteOnboardingUseCase;
import me.yeonjae.ovlo.application.port.in.member.RegisterMemberUseCase;
import me.yeonjae.ovlo.application.port.in.member.UpdateMemberProfileUseCase;
import me.yeonjae.ovlo.application.port.in.member.UpdateProfileImageUseCase;
import me.yeonjae.ovlo.application.port.out.auth.PasswordHasherPort;
import me.yeonjae.ovlo.application.port.out.media.MediaAccessPort;
import me.yeonjae.ovlo.domain.media.exception.MediaException;
import me.yeonjae.ovlo.application.port.out.member.HideContentByWithdrawnMemberPort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.member.SaveMemberPort;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityPort;
import me.yeonjae.ovlo.domain.member.exception.MemberException;
import me.yeonjae.ovlo.domain.member.model.*;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MemberCommandService implements
        RegisterMemberUseCase, UpdateMemberProfileUseCase, UpdateProfileImageUseCase,
        CompleteOnboardingUseCase {

    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;
    private final PasswordHasherPort passwordHasherPort;
    private final HideContentByWithdrawnMemberPort hideContentByWithdrawnMemberPort;
    private final LoadUniversityPort loadUniversityPort;
    private final MediaAccessPort mediaAccessPort;

    public MemberCommandService(LoadMemberPort loadMemberPort,
                                SaveMemberPort saveMemberPort,
                                PasswordHasherPort passwordHasherPort,
                                HideContentByWithdrawnMemberPort hideContentByWithdrawnMemberPort,
                                LoadUniversityPort loadUniversityPort,
                                MediaAccessPort mediaAccessPort) {
        this.loadMemberPort = loadMemberPort;
        this.saveMemberPort = saveMemberPort;
        this.passwordHasherPort = passwordHasherPort;
        this.hideContentByWithdrawnMemberPort = hideContentByWithdrawnMemberPort;
        this.loadUniversityPort = loadUniversityPort;
        this.mediaAccessPort = mediaAccessPort;
    }

    /** 본교가 카탈로그(global_universities)에 실재하는지 검증. */
    private void verifyUniversityExists(UniversityId universityId) {
        if (!loadUniversityPort.existsById(universityId)) {
            throw new MemberException(
                    "존재하지 않는 대학입니다: " + universityId.value(),
                    MemberException.ErrorType.NOT_FOUND);
        }
    }

    @Override
    public MemberResult register(RegisterMemberCommand command) {
        if (loadMemberPort.existsByEmail(command.email())) {
            throw new MemberException("이미 사용 중인 이메일입니다: " + command.email(), MemberException.ErrorType.CONFLICT);
        }
        if (loadMemberPort.existsByNickname(command.nickname())) {
            throw new MemberException("이미 사용 중인 닉네임입니다: " + command.nickname(), MemberException.ErrorType.CONFLICT);
        }

        verifyUniversityExists(new UniversityId(command.homeUniversityId()));

        String hashed = passwordHasherPort.encode(command.rawPassword());
        Major major = new Major(
                command.majorName(),
                DegreeType.valueOf(command.degreeType()),
                command.gradeLevel());

        Member member = Member.create(
                command.nickname(),
                command.name(),
                command.hometown(),
                new Email(command.email()),
                new Password(hashed),
                new UniversityId(command.homeUniversityId()),
                major);

        try {
            Member saved = saveMemberPort.save(member);
            return MemberResult.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new MemberException("이미 사용 중인 이메일 또는 닉네임입니다.", MemberException.ErrorType.CONFLICT);
        }
    }

    @Override
    public MemberResult updateProfile(UpdateMemberProfileCommand command) {
        Member member = loadMemberPort.findById(new MemberId(command.memberId()))
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다: " + command.memberId(), MemberException.ErrorType.NOT_FOUND));

        if (command.nickname() != null && !command.nickname().equals(member.getNickname())) {
            if (loadMemberPort.existsByNickname(command.nickname())) {
                throw new MemberException("이미 사용 중인 닉네임입니다: " + command.nickname(), MemberException.ErrorType.CONFLICT);
            }
            member.updateNickname(command.nickname());
        }
        if (command.bio() != null) {
            member.updateBio(command.bio());
        }

        try {
            Member saved = saveMemberPort.save(member);
            return MemberResult.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new MemberException("이미 사용 중인 닉네임입니다.", MemberException.ErrorType.CONFLICT);
        }
    }

    @Override
    public MemberResult updateProfileImage(UpdateProfileImageCommand command) {
        Long mediaId;
        try {
            mediaId = Long.valueOf(command.mediaId());
        } catch (NumberFormatException e) {
            throw new MediaException("사용할 수 없는 프로필 이미지입니다");
        }
        if (!mediaAccessPort.canUseAsProfileImage(mediaId, command.memberId())) {
            throw new MediaException("사용할 수 없는 프로필 이미지입니다");
        }
        Member member = loadMemberPort.findById(new MemberId(command.memberId()))
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다: " + command.memberId(), MemberException.ErrorType.NOT_FOUND));
        member.updateProfileImage(command.mediaId());
        Member saved = saveMemberPort.save(member);
        return MemberResult.from(saved);
    }

    /**
     * 탈퇴의 DB 부분. 진입점은 {@link MemberWithdrawalService} — Apple 회원은 거기서 Apple 토큰을 먼저
     * 폐기한 뒤에 이 메서드가 불린다(관리자 수동 탈퇴 {@code withdrawByAdmin} 만 폐기 없이 부른다).
     * 다른 경로에서 직접 부르지 말 것.
     */
    public void withdraw(WithdrawMemberCommand command) {
        Member member = loadMemberPort.findById(new MemberId(command.memberId()))
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다: " + command.memberId(), MemberException.ErrorType.NOT_FOUND));

        member.withdraw();
        saveMemberPort.save(member);
        hideContentByWithdrawnMemberPort.hideAllContentByMember(command.memberId());
    }

    @Override
    public void completeOnboarding(CompleteOnboardingCommand command) {
        Member member = loadMemberPort.findById(new MemberId(command.memberId()))
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다: " + command.memberId(), MemberException.ErrorType.NOT_FOUND));

        verifyUniversityExists(new UniversityId(command.homeUniversityId()));

        Major major = new Major(
                command.majorName(),
                DegreeType.valueOf(command.degreeType()),
                command.gradeLevel());

        member.completeOnboarding(command.hometown(), new UniversityId(command.homeUniversityId()), major);
        saveMemberPort.save(member);
    }
}
