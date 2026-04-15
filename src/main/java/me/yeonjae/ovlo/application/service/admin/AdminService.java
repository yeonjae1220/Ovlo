package me.yeonjae.ovlo.application.service.admin;

import me.yeonjae.ovlo.adapter.in.web.dto.response.AdminMemberResponse;
import me.yeonjae.ovlo.adapter.in.web.dto.response.AdminStatsResponse;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.member.SaveMemberPort;
import me.yeonjae.ovlo.domain.member.exception.MemberException;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;

    public AdminService(LoadMemberPort loadMemberPort, SaveMemberPort saveMemberPort) {
        this.loadMemberPort = loadMemberPort;
        this.saveMemberPort = saveMemberPort;
    }

    public Page<AdminMemberResponse> getMembers(Pageable pageable) {
        return loadMemberPort.findAll(pageable)
                .map(AdminMemberResponse::of);
    }

    @Transactional
    public AdminMemberResponse updateMemberRole(Long memberId, MemberRole role) {
        Member member = loadMemberPort.findById(new MemberId(memberId))
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다", MemberException.ErrorType.NOT_FOUND));
        member.updateRole(role);
        Member saved = saveMemberPort.save(member);
        return AdminMemberResponse.of(saved);
    }

    public AdminStatsResponse getStats() {
        return new AdminStatsResponse(loadMemberPort.count());
    }
}
