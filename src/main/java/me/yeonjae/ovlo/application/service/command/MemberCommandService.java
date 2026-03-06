package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.RegisterMemberCommand;
import me.yeonjae.ovlo.application.dto.command.UpdateMemberProfileCommand;
import me.yeonjae.ovlo.application.dto.command.WithdrawMemberCommand;
import me.yeonjae.ovlo.application.dto.result.MemberResult;
import me.yeonjae.ovlo.application.port.in.member.RegisterMemberUseCase;
import me.yeonjae.ovlo.application.port.in.member.UpdateMemberProfileUseCase;
import me.yeonjae.ovlo.application.port.in.member.WithdrawMemberUseCase;
import me.yeonjae.ovlo.application.port.out.auth.PasswordHasherPort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.member.SaveMemberPort;
import me.yeonjae.ovlo.domain.member.exception.MemberException;
import me.yeonjae.ovlo.domain.member.model.*;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MemberCommandService implements
        RegisterMemberUseCase, UpdateMemberProfileUseCase, WithdrawMemberUseCase {

    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;
    private final PasswordHasherPort passwordHasherPort;

    public MemberCommandService(LoadMemberPort loadMemberPort,
                                SaveMemberPort saveMemberPort,
                                PasswordHasherPort passwordHasherPort) {
        this.loadMemberPort = loadMemberPort;
        this.saveMemberPort = saveMemberPort;
        this.passwordHasherPort = passwordHasherPort;
    }

    @Override
    public MemberResult register(RegisterMemberCommand command) {
        if (loadMemberPort.existsByEmail(command.email())) {
            throw new MemberException("이미 사용 중인 이메일입니다: " + command.email());
        }

        String hashed = passwordHasherPort.encode(command.rawPassword());
        Major major = new Major(
                command.majorName(),
                DegreeType.valueOf(command.degreeType()),
                command.gradeLevel());

        Member member = Member.create(
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
            throw new MemberException("이미 사용 중인 이메일입니다: " + command.email());
        }
    }

    @Override
    public MemberResult updateProfile(UpdateMemberProfileCommand command) {
        Member member = loadMemberPort.findById(new MemberId(command.memberId()))
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다: " + command.memberId()));

        Major major = null;
        if (command.majorName() != null && command.degreeType() != null && command.gradeLevel() != null) {
            major = new Major(command.majorName(), DegreeType.valueOf(command.degreeType()), command.gradeLevel());
        }

        member.updateProfile(command.name(), command.hometown(), major);
        Member saved = saveMemberPort.save(member);
        return MemberResult.from(saved);
    }

    @Override
    public void withdraw(WithdrawMemberCommand command) {
        Member member = loadMemberPort.findById(new MemberId(command.memberId()))
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다: " + command.memberId()));

        member.withdraw();
        saveMemberPort.save(member);
    }
}
