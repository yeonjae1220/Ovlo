package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.dto.result.OfficeResult;
import me.yeonjae.ovlo.application.dto.result.OfficeStaffResult;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityPort;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.OfficeStaff;
import me.yeonjae.ovlo.domain.office.model.OfficeStaffStatus;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Office 애그리거트 → 결과 DTO. 대학 이름과 담당자 이메일·닉네임을 붙인다. */
@Component
public class OfficeResultMapper {

    private final LoadUniversityPort loadUniversityPort;
    private final LoadMemberPort loadMemberPort;

    public OfficeResultMapper(LoadUniversityPort loadUniversityPort, LoadMemberPort loadMemberPort) {
        this.loadUniversityPort = loadUniversityPort;
        this.loadMemberPort = loadMemberPort;
    }

    public OfficeResult toResult(Office office) {
        String universityName = loadUniversityPort.findById(new UniversityId(office.getGlobalUnivId()))
                .map(u -> u.getName())
                .orElse("(알 수 없음, id=" + office.getGlobalUnivId() + ")");
        List<OfficeStaff> visible = office.getStaff().stream()
                .filter(s -> s.getStatus() != OfficeStaffStatus.REMOVED)
                .toList();
        Map<Long, Member> members = loadMemberPort.findAllByIds(
                        visible.stream().map(s -> new MemberId(s.getMemberId())).toList()).stream()
                .collect(Collectors.toMap(m -> m.getId().value(), Function.identity()));
        List<OfficeStaffResult> staff = visible.stream()
                .map(s -> toStaffResult(s, members.get(s.getMemberId())))
                .toList();
        return new OfficeResult(office.getId() != null ? office.getId().value() : null,
                office.getGlobalUnivId(), universityName, office.getName(), office.getStatus(),
                office.getCreatedAt(), staff);
    }

    public OfficeStaffResult toStaffResult(OfficeStaff s, Member member) {
        return new OfficeStaffResult(s.getMemberId(),
                member != null ? member.getEmail().value() : null,
                member != null ? member.getNickname() : null,
                s.getRole(), s.getStatus(), s.getInvitedAt(), s.getAcceptedAt());
    }
}
