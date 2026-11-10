package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.*;
import me.yeonjae.ovlo.application.dto.result.*;
import me.yeonjae.ovlo.application.port.in.calendar.UniversityApplicationUseCase;
import me.yeonjae.ovlo.application.port.out.auth.PasswordHasherPort;
import me.yeonjae.ovlo.application.port.out.calendar.CalendarMembershipPort;
import me.yeonjae.ovlo.application.port.out.member.*;
import me.yeonjae.ovlo.domain.calendar.model.*;
import me.yeonjae.ovlo.domain.calendar.exception.CalendarException;
import me.yeonjae.ovlo.domain.member.model.*;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static me.yeonjae.ovlo.domain.calendar.exception.CalendarException.ErrorType.*;

@Service
@Transactional
public class UniversityApplicationService implements UniversityApplicationUseCase {
    private final CalendarMembershipPort memberships;
    private final CalendarAccessService access;
    private final LoadMemberPort members;
    private final SaveMemberPort saveMembers;
    private final PasswordHasherPort passwords;
    public UniversityApplicationService(CalendarMembershipPort memberships, CalendarAccessService access, LoadMemberPort members,
                                         SaveMemberPort saveMembers, PasswordHasherPort passwords) {
        this.memberships = memberships; this.access = access; this.members = members; this.saveMembers = saveMembers; this.passwords = passwords;
    }
    @Override public MemberResult register(UniversityRegistrationCommand c) {
        access.school(c.universityId());
        if (members.existsByEmail(c.email()) || members.existsByNickname(c.nickname())) throw new CalendarException("이미 사용 중인 이메일 또는 닉네임입니다", CONFLICT);
        var member = saveMembers.save(Member.createUniversityApplicant(c.nickname(), c.name(), new Email(c.email()),
                new Password(passwords.encode(c.password())), new UniversityId(c.universityId())));
        memberships.saveApplication(UniversityApplication.apply(member.getId().value(), c.universityId(), c.department(), c.email(), c.message()));
        return MemberResult.from(member);
    }
    @Override public UniversityApplicationResult apply(long memberId, long schoolId, String department, String email, String message) {
        access.active(memberId); access.school(schoolId);
        if (memberships.hasOpenApplication(memberId, schoolId) || memberships.manages(memberId, schoolId))
            throw new CalendarException("이미 대기 중이거나 승인된 신청이 있습니다", CONFLICT);
        return result(memberships.saveApplication(UniversityApplication.apply(memberId, schoolId, department, email, message)));
    }
    @Override @Transactional(readOnly = true) public List<UniversityApplicationResult> mine(long memberId) {
        access.active(memberId); return memberships.applications(memberId, null, 0, 100).stream().map(this::result).toList();
    }
    @Override @Transactional(readOnly = true) public List<UniversityApplicationResult> list(CalendarActor actor, String status, int page) {
        access.admin(actor);
        if (page < 0 || page > 10000) throw new IllegalArgumentException("잘못된 페이지입니다");
        return memberships.applications(null, status == null || status.isBlank() ? null : ApplicationStatus.valueOf(status), page, 30).stream().map(this::result).toList();
    }
    @Override public void review(CalendarActor actor, long id, String action, String note, long version) {
        access.admin(actor);
        var app = memberships.findApplication(id).orElseThrow(() -> new CalendarException("신청을 찾을 수 없습니다", NOT_FOUND));
        CalendarAccessService.version(app.version(), version);
        if ("revoke".equals(action)) {
            app.revoke(note, actor.auditName()); memberships.revoke(app.memberId(), app.universityId());
            // Keep the account role; removed school assignments are checked live on every write.
        } else if ("approve".equals(action) || "reject".equals(action)) {
            boolean approved = "approve".equals(action);
            app.review(approved, note, actor.auditName());
            if (approved) {
                var member = access.active(app.memberId());
                if (member.getRole() == MemberRole.MEMBER) { member.updateRole(MemberRole.UNIV); saveMembers.save(member); }
                memberships.grant(app.memberId(), app.universityId());
            }
        } else throw new IllegalArgumentException("지원하지 않는 심사 작업입니다");
        memberships.saveApplication(app);
    }
    private UniversityApplicationResult result(UniversityApplication app) {
        String name = members.findById(new MemberId(app.memberId())).map(Member::getName).orElse("탈퇴 회원");
        return UniversityApplicationResult.from(app, name, access.school(app.universityId()).getName());
    }
}
