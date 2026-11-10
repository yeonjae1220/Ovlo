package me.yeonjae.ovlo.application.service.command;
import me.yeonjae.ovlo.application.dto.command.CalendarActor;
import me.yeonjae.ovlo.application.port.out.calendar.CalendarMembershipPort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityPort;
import me.yeonjae.ovlo.domain.calendar.exception.CalendarException;
import me.yeonjae.ovlo.domain.member.model.*;
import me.yeonjae.ovlo.domain.university.model.*;
import org.springframework.stereotype.Service;
import static me.yeonjae.ovlo.domain.calendar.exception.CalendarException.ErrorType.*;

@Service
public class CalendarAccessService {
    private final LoadMemberPort members;
    private final LoadUniversityPort schools;
    private final CalendarMembershipPort memberships;
    public CalendarAccessService(LoadMemberPort members, LoadUniversityPort schools, CalendarMembershipPort memberships) {
        this.members = members; this.schools = schools; this.memberships = memberships;
    }
    public Member active(Long id) {
        if (id == null) throw new CalendarException("로그인이 필요합니다", UNAUTHORIZED);
        Member member = members.findById(new MemberId(id)).orElseThrow(() -> new CalendarException("회원을 찾을 수 없습니다", UNAUTHORIZED));
        if (!member.getStatus().isActive()) throw new CalendarException("활성 회원만 이용할 수 있습니다", FORBIDDEN);
        return member;
    }
    public University school(long id) {
        return schools.findById(new UniversityId(id)).orElseThrow(() -> new CalendarException("학교를 찾을 수 없습니다", NOT_FOUND));
    }
    public boolean isAdmin(CalendarActor actor) {
        return actor.administrator() != null || active(actor.memberId()).getRole() == MemberRole.ADMIN;
    }
    public void admin(CalendarActor actor) {
        if (!isAdmin(actor)) throw new CalendarException("운영진 권한이 필요합니다", FORBIDDEN);
    }
    public void manage(CalendarActor actor, long schoolId) {
        if (actor.administrator() != null) { school(schoolId); return; }
        Member member = active(actor.memberId());
        if (member.getRole() != MemberRole.ADMIN && (member.getRole() != MemberRole.UNIV || !memberships.manages(member.getId().value(), schoolId)))
            throw new CalendarException("이 학교의 일정 관리 권한이 없습니다", FORBIDDEN);
        school(schoolId);
    }
    public static void version(long actual, Long expected) {
        if (expected == null || expected != actual) throw new CalendarException("일정 또는 신청이 변경되었습니다. 새로고침 후 다시 시도해 주세요", CONFLICT);
    }
}
